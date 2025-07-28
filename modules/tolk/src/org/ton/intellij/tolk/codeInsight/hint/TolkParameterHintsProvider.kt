package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkReferenceElement
import org.ton.intellij.tolk.psi.impl.functionSymbol
import org.ton.intellij.tolk.psi.unwrapParentheses
import org.ton.intellij.util.printPsi

class TolkParameterHintsProvider : AbstractTolkInlayHintProvider() {
    override fun collectFromElement(
        element: PsiElement,
        sink: InlayTreeSink,
    ) {
        if (element !is TolkCallExpression) return

        val function = element.functionSymbol
        if (function?.name == "ton") {
            // Obvious and `floatString:` is too long for a parameter hint here
            return
        }

        iterateOverParameters(
            element
        ) { parameter, argument ->
            val parameterName = parameter.name ?: return@iterateOverParameters
            val expression = argument?.expression?.unwrapParentheses() ?: return@iterateOverParameters

            if (needParameterHint(expression, parameterName)) {
                sink.addPresentation(
                    position = InlineInlayPosition(argument.textRange.startOffset, false),
                    hasBackground = true,
                ) {
                    printPsi(parameter, parameterName)
                    text(":")
                }
            }
        }
    }

    private fun needParameterHint(
        expression: TolkExpression,
        parameterName: String,
    ): Boolean {
        if (parameterName.length == 1) {
            // no need to show a hint for single letter parameters as it does not add any information to the reader
            return false
        }

        if (expression is TolkReferenceElement) {
            // no need to show a hint for `takeFoo(foo)`
            return expression.referenceName != parameterName
        }

        if (expression is TolkDotExpression) {
            // no need to show a hint for `takeFoo(obj.foo)`
            return expression.fieldLookup?.referenceName != parameterName
        }

        if (expression is TolkCallExpression) {
            // no need to show a hint for `takeFoo(foo())`
            return expression.functionSymbol?.name != parameterName
        }

        return true
    }
}
