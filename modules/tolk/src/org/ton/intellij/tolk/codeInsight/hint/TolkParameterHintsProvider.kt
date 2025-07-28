package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkReferenceElement
import org.ton.intellij.tolk.psi.impl.functionSymbol
import org.ton.intellij.tolk.psi.unwrapParentheses
import org.ton.intellij.util.printPsi

class TolkParameterHintsProvider : AbstractTolkInlayHintProvider() {
    override fun collectFromElement(
        element: PsiElement,
        sink: InlayTreeSink
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

            // There is no need to show a hint for single letter parameters as it does not add any information to the reader
            if (parameterName.length == 1) return@iterateOverParameters

            val expression = argument?.expression?.unwrapParentheses() ?: return@iterateOverParameters
            if (expression !is TolkReferenceElement || expression.referenceName != parameterName) {
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
}
