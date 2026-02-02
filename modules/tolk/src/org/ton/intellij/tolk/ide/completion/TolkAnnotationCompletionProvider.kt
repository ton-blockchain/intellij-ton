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
import org.ton.intellij.tolk.psi.TolkAnnotationHolder
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.impl.isEntryPoint
import org.ton.intellij.tolk.psi.impl.isGetMethod

typealias Applicability = (PsiElement) -> Boolean

object TolkAnnotationCompletionProvider : TolkCompletionProvider(), DumbAware {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = psiElement().afterLeaf("@")

    val forAny: Applicability = { true }
    val forFunctions: Applicability = { it is TolkFunction }
    val forGetMethods: Applicability = { it is TolkFunction && it.isGetMethod }
    val forStructs: Applicability = { it is TolkStruct }
    val forEntryPoints: Applicability = { it is TolkFunction && it.isEntryPoint }

    private val lookupElements = listOf(
        LookupElementBuilder.create("pure") to forFunctions,
        LookupElementBuilder.create("noinline") to forFunctions,
        LookupElementBuilder.create("inline") to forFunctions,
        LookupElementBuilder.create("inline_ref") to forFunctions,
        LookupElementBuilder.create("test").withInsertHandler(ParInsertHandler) to forGetMethods,
        LookupElementBuilder.create("method_id").withInsertHandler(ParInsertHandler) to forFunctions,
        LookupElementBuilder.create("abi").withInsertHandler(ParInsertHandler) to forAny,
        LookupElementBuilder.create("deprecated").withTailText("(\"reason\")")
            .withInsertHandler(StringArgumentInsertHandler("")) to forAny,
        LookupElementBuilder.create("custom").withInsertHandler(ParInsertHandler) to forAny,
        LookupElementBuilder.create("on_bounced_policy").withTailText("(\"manual\")")
            .withInsertHandler(StringArgumentInsertHandler("manual")) to forEntryPoints,
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
            } to forStructs
    ).map {
        it.first.withIcon(TolkIcons.ANNOTATION) to it.second
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val owner = parameters.position.parent.parent
        if (owner is TolkAnnotationHolder) {
            val currentAnnotations = owner.annotations.names().toSet()
            result.addAllElements(lookupElements.filter { (element, isApplicable) ->
                !currentAnnotations.contains(element.lookupString) && isApplicable(owner)
            }.map { it.first })
            return
        }

        result.addAllElements(lookupElements.map { it.first })
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
