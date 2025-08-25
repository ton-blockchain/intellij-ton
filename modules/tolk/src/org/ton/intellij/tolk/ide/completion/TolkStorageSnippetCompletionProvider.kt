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
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkFile

object TolkStorageSnippetCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        PlatformPatterns.psiElement()
            .withSuperParent(2, TolkFile::class.java)
            .with(object : PatternCondition<PsiElement>("atLineStart") {
                override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
                    // accept
                    // <caret>
                    // but not
                    // fun <caret>
                    return t.prevSibling == null
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
    }
}