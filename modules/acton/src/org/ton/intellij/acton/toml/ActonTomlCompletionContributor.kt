package org.ton.intellij.acton.toml

import com.google.gson.Gson
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTable
import org.ton.intellij.util.psiElement

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
                    val file = parameters.originalFile
                    if (file.name != "Acton.toml") return

                    val element = parameters.position.parent as? TomlKeySegment ?: return
                    val table = element.parent?.parent?.parent as? TomlTable ?: return
                    val header = table.header
                    val segments = header.key?.segments ?: return
                    if (segments.isEmpty() || segments[0].name != "lint") return

                    val project = parameters.editor.project ?: return
                    val rules = ActonLintRulesProvider.getLintRules(project)

                    for (rule in rules) {
                        val description = rule.description.lineSequence().firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                            ?: ""

                        result.addElement(
                            LookupElementBuilder.create(rule.name)
                                .withInsertHandler { context, _ ->
                                    context.document.insertString(context.selectionEndOffset, " = \"\"")
                                    context.editor.caretModel.moveToOffset(
                                        context.editor.caretModel.offset + 4
                                    )
                                    AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
                                }
                                .withTypeText("lint rule")
                                .withTailText(" $description", true)
                        )
                    }
                }
            }
        )
    }
}
