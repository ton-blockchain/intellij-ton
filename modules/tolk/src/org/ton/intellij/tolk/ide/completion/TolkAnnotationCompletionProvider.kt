package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.TolkIcons

object TolkAnnotationCompletionProvider : TolkCompletionProvider(), DumbAware {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = psiElement().afterLeaf("@")

    private val lookupElements = listOf(
        LookupElementBuilder.create("pure"),
        LookupElementBuilder.create("noinline"),
        LookupElementBuilder.create("inline"),
        LookupElementBuilder.create("inline_ref"),
        LookupElementBuilder.create("method_id").withInsertHandler(ParInsertHandler),
        LookupElementBuilder.create("deprecated").withTailText("(\"reason\")").withInsertHandler(StringArgumentInsertHandler("")),
        LookupElementBuilder.create("custom").withInsertHandler(ParInsertHandler),
        LookupElementBuilder.create("on_bounced_policy").withTailText("(\"manual\")")
            .withInsertHandler(StringArgumentInsertHandler("manual")),
        LookupElementBuilder.create("overflow1023_policy")
            .withTailText("(\"suppress\")")
            .withInsertHandler { ctx, item ->
                ParInsertHandler.handleInsert(ctx, item)
                // Проверяем, находится ли курсор внутри пустых скобок
                val offset = ctx.editor.caretModel.offset
                val chars = ctx.document.charsSequence

                if (offset < chars.length && chars[offset] == ')' && offset > 0 && chars[offset - 1] == '(') {
                    ctx.document.insertString(offset, "\"suppress\"")
                    ctx.editor.caretModel.moveToOffset(offset + "\"suppress\")".length)
                }
            }
    ).map {
        it.withIcon(TolkIcons.ANNOTATION)
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        result.addAllElements(lookupElements)
    }

    private object ParInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(
            context: InsertionContext,
            item: LookupElement,
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

    private class StringArgumentInsertHandler(private val value: String) : InsertHandler<LookupElement> {
        override fun handleInsert(
            context: InsertionContext,
            item: LookupElement,
        ) {
            TemplateStringInsertHandler("(\"\$value$\")", true, "value" to ConstantNode(value)).handleInsert(context, item)
        }
    }
}
