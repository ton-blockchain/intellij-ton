package org.ton.intellij.acton.toml

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable
import org.ton.intellij.acton.cli.ActonToml

class ActonTomlCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            psiElement().with(object : PatternCondition<PsiElement>("inFunctionDeclaration") {
                override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
                    val parent = t.parent
                    return parent is TomlKeySegment
                }
            }),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    addKeyCompletions(parameters, result)
                }
            },
        )

        extend(
            CompletionType.BASIC,
            psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    addValueCompletions(parameters, result)
                }
            },
        )
    }

    private fun addKeyCompletions(parameters: CompletionParameters, result: CompletionResultSet) {
        val file = parameters.originalFile
        if (file.name != "Acton.toml") return

        val element = parameters.position.parent as? TomlKeySegment ?: return
        val table = element.parent?.parent?.parent as? TomlTable ?: return
        val segments = table.header.key?.segments ?: return
        val project = parameters.editor.project ?: return

        when {
            segments.size == 2 && segments[0].name == "lint" && segments[1].name == "rules" -> {
                val rules = ActonLintRulesProvider.getLintRules(project)
                for (rule in rules) {
                    val description =
                        rule.description.lineSequence().firstOrNull { it.isNotBlank() && !it.startsWith("#") } ?: ""
                    result.addElement(
                        LookupElementBuilder.create(rule.name)
                            .withInsertHandler { insertionContext, _ ->
                                insertionContext.document.insertString(insertionContext.selectionEndOffset, " = \"\"")
                                insertionContext.editor.caretModel.moveToOffset(
                                    insertionContext.editor.caretModel.offset + 4,
                                )
                                AutoPopupController.getInstance(insertionContext.project)
                                    .scheduleAutoPopup(insertionContext.editor)
                            }
                            .withTypeText("lint rule")
                            .withTailText(" $description", true),
                    )
                }
            }

            segments.size == 3 && segments[0].name == "lint" && segments[1].name == "rules" -> {
                val actonToml = ActonToml.find(project) ?: return
                for (contractId in actonToml.getContractIds().distinct()) {
                    result.addElement(
                        LookupElementBuilder.create(contractId)
                            .withIcon(AllIcons.Nodes.Class)
                            .withInsertHandler { insertionContext, _ ->
                                insertionContext.document.insertString(insertionContext.selectionEndOffset, " = \"\"")
                                insertionContext.editor.caretModel.moveToOffset(
                                    insertionContext.editor.caretModel.offset + 4,
                                )
                                AutoPopupController.getInstance(insertionContext.project)
                                    .scheduleAutoPopup(insertionContext.editor)
                            }
                            .withTypeText("contract override"),
                    )
                }
            }
        }
    }

    private fun addValueCompletions(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.originalFile.name != "Acton.toml") return

        val literal = PsiTreeUtil.getParentOfType(parameters.position, TomlLiteral::class.java, false) ?: return
        val valueContext = findActonTomlValueContext(literal) ?: return
        val project = parameters.editor.project ?: return
        val actonToml = ActonToml.find(project) ?: return

        when {
            valueContext.matches("litenode", "accounts") && valueContext.isArrayItem -> {
                for (wallet in actonToml.getWallets().distinctBy { it.name }) {
                    result.addElement(
                        LookupElementBuilder.create(wallet.name)
                            .withIcon(AllIcons.General.User)
                            .withTailText(if (wallet.isLocal) " (local)" else " (global)", true),
                    )
                }
            }

            (valueContext.matches("contracts", null, "depends") && valueContext.isArrayItem) ||
                valueContext.matches("contracts", null, "depends", "name") -> {
                val currentContractId = valueContext.path.getOrNull(1)
                for (contractId in actonToml.getContractIds().distinct()) {
                    if (contractId == currentContractId) continue
                    result.addElement(
                        LookupElementBuilder.create(contractId)
                            .withIcon(AllIcons.Nodes.Class),
                    )
                }
            }

            valueContext.matches("test", "fork-net", "custom") ||
                valueContext.matches("litenode", "fork-net", "custom") -> {
                for (networkName in actonToml.getCustomNetworkNames()) {
                    result.addElement(LookupElementBuilder.create(networkName).withTypeText("custom network"))
                }
            }

            valueContext.matches("test", "mutation", "disable-rules") && valueContext.isArrayItem -> {
                for (ruleId in MUTATION_RULE_IDS) {
                    result.addElement(
                        LookupElementBuilder.create(ruleId)
                            .withTypeText("mutation rule"),
                    )
                }
            }

            valueContext.matches("package", "license") -> {
                for (license in LICENSE_OPTIONS) {
                    result.addElement(
                        LookupElementBuilder.create(license)
                            .withTypeText("license"),
                    )
                }
            }

            valueContext.isGlobField() -> addGlobPathCompletions(valueContext, actonToml, result, parameters)
        }
    }

    private fun addGlobPathCompletions(
        valueContext: ActonTomlValueContext,
        actonToml: ActonToml,
        result: CompletionResultSet,
        parameters: CompletionParameters,
    ) {
        val root = actonToml.virtualFile.parent ?: return
        val normalizedPrefix = extractTypedLiteralPrefix(valueContext.literal, parameters).replace('\\', '/')
        if (normalizedPrefix.any { it == '*' || it == '?' || it == '[' || it == ']' || it == '{' || it == '}' }) return

        val baseDirPath = normalizedPrefix.substringBeforeLast('/', "")
        val namePrefix = normalizedPrefix.substringAfterLast('/', normalizedPrefix)
        val container = resolveContainer(root, baseDirPath) ?: return
        val pathPrefix = if (baseDirPath.isEmpty()) "" else "$baseDirPath/"

        for (child in container.children.sortedBy { it.name.lowercase() }) {
            if (!child.name.startsWith(namePrefix)) continue
            val relativePath = pathPrefix + child.name
            if (child.isDirectory) {
                result.addElement(
                    LookupElementBuilder.create("$relativePath/")
                        .withLookupString(child.name)
                        .withLookupString("${child.name}/")
                        .withIcon(AllIcons.Nodes.Folder)
                        .withTypeText("directory")
                        .withInsertHandler { insertionContext, _ ->
                            AutoPopupController.getInstance(
                                insertionContext.project,
                            ).scheduleAutoPopup(insertionContext.editor)
                        },
                )
                result.addElement(
                    LookupElementBuilder.create("$relativePath/**")
                        .withLookupString(child.name)
                        .withLookupString("${child.name}/**")
                        .withIcon(AllIcons.Nodes.Folder)
                        .withTypeText("glob")
                        .withTailText(" recursive", true),
                )
            } else {
                result.addElement(
                    LookupElementBuilder.create(relativePath)
                        .withLookupString(child.name)
                        .withIcon(child.fileType.icon ?: AllIcons.FileTypes.Any_type)
                        .withTypeText("file"),
                )
            }
        }
    }

    private fun resolveContainer(root: VirtualFile, baseDirPath: String): VirtualFile? {
        if (baseDirPath.isEmpty()) return root
        return root.findFileByRelativePath(baseDirPath)
            ?.takeIf { it.isDirectory }
    }

    private fun extractTypedLiteralPrefix(literal: TomlLiteral, parameters: CompletionParameters): String {
        val content = literal.text.removeSurrounding("\"").removeSurrounding("'")
        val contentStartOffset = literal.textRange.startOffset + 1
        val rawPrefixLength = (parameters.offset - contentStartOffset).coerceIn(0, content.length)
        return content.substring(0, rawPrefixLength)
            .removeSuffix(CompletionInitializationContext.DUMMY_IDENTIFIER)
            .removeSuffix(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED)
            .removeSuffix("IntellijIdeaRulezzz ")
            .removeSuffix("IntellijIdeaRulezzz")
    }

    private fun ActonTomlValueContext.isGlobField(): Boolean = (
        matches("fmt", "ignore") ||
            matches("lint", "exclude") ||
            matches("test", "include") ||
            matches("test", "exclude")
        ) &&
        isArrayItem

    companion object {
        private val LICENSE_OPTIONS = listOf(
            "MIT",
            "Apache-2.0",
            "GPL-3.0",
            "BSD-3-Clause",
            "ISC",
            "Unlicense",
        )

        private val MUTATION_RULE_IDS = listOf(
            "remove_assert",
            "remove_throw",
            "remove_storage_save_call",
            "remove_set_data_call",
            "remove_accept_external_message",
            "remove_commit_contract_data_and_actions",
            "remove_set_code_postponed",
            "if_condition_true",
            "if_condition_false",
            "while_condition_false",
            "remove_logical_not",
            "flip_plus",
            "flip_minus",
            "flip_mul_div",
            "flip_div_mul",
            "flip_mul_assign",
            "flip_div_assign",
            "flip_mul_assign_mod",
            "flip_mod_assign",
            "flip_plus_assign",
            "flip_minus_assign",
            "remove_unary_minus",
            "flip_eq_ne",
            "flip_ne_eq",
            "flip_lt_le",
            "flip_gt_ge",
            "flip_le_lt",
            "flip_ge_gt",
            "invert_bool_true",
            "invert_bool_false",
            "flip_logical_and",
            "flip_logical_or",
            "flip_bitwise_and",
            "flip_bitwise_or",
            "flip_bitwise_and_xor",
            "flip_bitwise_or_xor",
            "flip_bitwise_xor",
            "flip_bitwise_xor_or",
            "flip_bitwise_and_assign_or",
            "flip_bitwise_and_assign_xor",
            "flip_bitwise_or_assign_and",
            "flip_bitwise_or_assign_xor",
            "flip_bitwise_xor_assign_and",
            "flip_bitwise_xor_assign_or",
            "flip_lshift_assign",
            "flip_rshift_assign",
            "flip_lshift",
            "flip_rshift",
            "remove_bitwise_not",
        )
    }
}
