package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

object TolkAnnotationCompletionProvider : TolkCompletionProvider(), DumbAware {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = psiElement().afterLeaf("@")

    private val lookupElements = listOf(
        LookupElementBuilder.create("pure"),
        LookupElementBuilder.create("noinline"),
        LookupElementBuilder.create("inline"),
        LookupElementBuilder.create("inline_ref"),
        LookupElementBuilder.create("method_id").withInsertHandler(ParInsertHandler),
        LookupElementBuilder.create("deprecated"),
        LookupElementBuilder.create("custom").withInsertHandler(ParInsertHandler),
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        lookupElements.forEach {
            result.addElement(it)
        }
    }

    private object ParInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(
            context: InsertionContext,
            item: LookupElement
        ) {
            val offset = context.editor.caretModel.offset
            val chars = context.document.charsSequence
            val absoluteOpeningBracketOffset = chars.indexOfSkippingSpace('(', offset)

            if (absoluteOpeningBracketOffset == null) {
//                val offset = if (this.parameterList?.parameterList.isNullOrEmpty()) 2 else 1
                val offset = 1
                context.editor.document.insertString(context.editor.caretModel.offset, "()")
                context.editor.caretModel.moveToOffset(context.editor.caretModel.offset + offset)
                context.commitDocument()
            }
        }
    }
}
