package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.icons.AllIcons
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.util.prevLeaf

object TolkTopLevelSnippetsCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        PlatformPatterns.psiElement()
            .withSuperParent(2, TolkFile::class.java)
            .with(object : PatternCondition<PsiElement>("atLineStart") {
                override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
                    // accept
                    // <caret>
                    // but not
                    // fun <caret>
                    return t.prevSibling == null && t.prevLeaf is PsiWhiteSpace
                }
            })

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        result.addElement(
            LookupElementBuilder.create("storage")
                .withIcon(AllIcons.Actions.RealIntentionBulb)
                .withTailText(" Generates storage struct and methods", true)
                .withInsertHandler { context, item ->
                    // We need to remove the automatically inserted `storage`
                    // and insert the correct text instead
                    val document = context.document
                    val start = context.startOffset
                    document.deleteString(start, start + "storage".length)
                    TemplateStringInsertHandler(
                        """
                            struct ${'$'}name$ {
                                ${'$'}END$
                            }
        
                            fun ${'$'}name$.load() {
                                return ${'$'}name$.fromCell(contract.getData());
                            }
        
                            fun ${'$'}name$.save(self) {
                                contract.setData(self.toCell());
                            }
                        """.trimIndent(), true, "name" to ConstantNode("Storage")
                    ).handleInsert(context, item)
                }
        )

        result.addElement(
            LookupElementBuilder.create("contract")
                .withIcon(AllIcons.Actions.RealIntentionBulb)
                .withTailText(" Generates contract header", true)
                .withInsertHandler { context, item ->
                    val document = context.document
                    val start = context.startOffset
                    document.deleteString(start, start + "contract".length)

                    val fileName = context.file.name.removeSuffix(".tolk")
                    val pascalName = fileName.split('-', '_', ' ')
                        .filter { it.isNotEmpty() }
                        .joinToString("") { it.replaceFirstChar { char -> char.uppercaseChar() } }
                        .ifEmpty { "Main" }

                    TemplateStringInsertHandler(
                        """
                            contract ${'$'}name$ {
                                version: "${'$'}version$"
                                description: "${'$'}description$"
                                incomingMessages: ${'$'}messages$
                                storage: ${'$'}storage$
                            }
                        """.trimIndent(), true,
                        "name" to ConstantNode(pascalName),
                        "version" to ConstantNode("1.0.0"),
                        "description" to ConstantNode("My TON contract"),
                        "messages" to ConstantNode("AllowedMessages"),
                        "storage" to ConstantNode("Storage")
                    ).handleInsert(context, item)
                }
        )

        val file = parameters.originalFile as? TolkFile ?: return
        val firstType = file.structs.firstOrNull() ?: file.typeDefs.firstOrNull() ?: file.enums.firstOrNull()

        result.addElement(
            LookupElementBuilder.create("method fun")
                .withIcon(AllIcons.Actions.RealIntentionBulb)
                .withTailText(" Generates instance method", true)
                .withInsertHandler { context, item ->
                    // We need to remove the automatically inserted `method fun`
                    // and insert the correct text instead
                    val document = context.document
                    val start = context.startOffset
                    document.deleteString(start, start + "method fun".length)
                    TemplateStringInsertHandler(
                        "fun \$type$.\$name$(self\$params$)\$return$ {\n\$END$\n}", true,
                        "type" to ConstantNode(firstType?.name ?: "Foo"),
                        "name" to ConstantNode("name"),
                        "params" to ConstantNode(""),
                        "return" to ConstantNode(""),
                    ).handleInsert(context, item)
                }
        )

        result.addElement(
            LookupElementBuilder.create("static method fun")
                .withIcon(AllIcons.Actions.RealIntentionBulb)
                .withTailText(" Generates static method", true)
                .withInsertHandler { context, item ->
                    // We need to remove the automatically inserted `static method fun`
                    // and insert the correct text instead
                    val document = context.document
                    val start = context.startOffset
                    document.deleteString(start, start + "static method fun".length)
                    TemplateStringInsertHandler(
                        "fun \$type$.\$name$(\$params$)\$return$ {\n\$END$\n}", true,
                        "type" to ConstantNode(firstType?.name ?: "Foo"),
                        "name" to ConstantNode("name"),
                        "params" to ConstantNode(""),
                        "return" to ConstantNode(""),
                    ).handleInsert(context, item)
                }
        )
    }
}
