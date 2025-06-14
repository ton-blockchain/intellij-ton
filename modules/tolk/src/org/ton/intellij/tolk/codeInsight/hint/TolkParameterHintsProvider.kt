package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkReferenceElement
import org.ton.intellij.tolk.psi.unwrapParentheses
import org.ton.intellij.util.printPsi

class TolkParameterHintsProvider : AbstractTolkInlayHintProvider() {
    override fun collectFromElement(
        element: PsiElement,
        sink: InlayTreeSink
    ) {
        if (element !is TolkCallExpression) return
        iterateOverParameters(
            element
        ) { parameter, argument ->
            val parameterName = parameter.name ?: return@iterateOverParameters
            val expression = argument?.expression?.unwrapParentheses() ?: return@iterateOverParameters
            if (expression !is TolkReferenceElement || expression.referenceName != parameterName) {
                sink.addPresentation(
                    position = InlineInlayPosition(argument.textRange.startOffset, false),
                    hasBackground = true,
                ) {
                    printPsi(parameter, parameterName)
                    text(" = ")
                }
            }
        }
    }
}
