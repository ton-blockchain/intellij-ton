package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.ide.completion.TolkLookupElementData.KeywordKind.KEYWORD
import org.ton.intellij.tolk.ide.completion.TolkLookupElementData.KeywordKind.CONTEXT_RETURN_KEYWORD
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.impl.returnTy
import org.ton.intellij.tolk.type.TolkIntTyFamily
import org.ton.intellij.tolk.type.TolkTyBool
import org.ton.intellij.tolk.type.TolkTyUnion
import org.ton.intellij.tolk.type.TolkTyVoid
import org.ton.intellij.util.parentOfType

object TolkReturnCompletionProvider : TolkCompletionProvider(), DumbAware {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = TolkCompletionPatterns.inBlock()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val outerFunction = parameters.position.parentOfType<TolkFunction>() ?: return

        if (outerFunction.returnType == null) {
            // no explicit return type
            result.addElement(
                LookupElementBuilder.create("return")
                    .bold()
                    .withTailText(" expr;", true)
                    .withInsertHandler(
                        TemplateStringInsertHandler(
                            " \$expr$;", true, "expr" to ConstantNode("")
                        )
                    )
                    .toTolkLookupElement(TolkLookupElementData(keywordKind = KEYWORD))
            )
        }

        val returnTy = outerFunction.returnTy
        if (returnTy is TolkTyVoid) {
            result.addElement(
                LookupElementBuilder.create("return;")
                    .bold()
                    .toTolkLookupElement(TolkLookupElementData(keywordKind = KEYWORD))
            )
            return
        }

        result.addElement(
            LookupElementBuilder.create("return")
                .bold()
                .withTailText(" expr;", true)
                .withInsertHandler(
                    TemplateStringInsertHandler(
                        " \$expr$;", true, "expr" to ConstantNode("")
                    )
                )
                .toTolkLookupElement(TolkLookupElementData(keywordKind = KEYWORD)) // lower priority than CONTEXT_RETURN_KEYWORD
        )

        if (returnTy is TolkTyBool) {
            result.addElement(createReturnExprElement2("true"))
            result.addElement(createReturnExprElement2("false"))
        }

        if (returnTy is TolkIntTyFamily) {
            result.addElement(createReturnExprElement2("0"))
        }

        if (returnTy is TolkTyUnion && returnTy.isNullable()) {
            result.addElement(createReturnExprElement2("null"))
        }
    }

    private fun createReturnExprElement2(value: String): TolkLookupElement = LookupElementBuilder.create("return ${value};")
        .bold()
        .toTolkLookupElement(TolkLookupElementData(keywordKind = CONTEXT_RETURN_KEYWORD))
}
