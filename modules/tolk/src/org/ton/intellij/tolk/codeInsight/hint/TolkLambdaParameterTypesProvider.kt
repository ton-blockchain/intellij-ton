package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkLambdaFunExpression
import org.ton.intellij.tolk.type.TolkTyUnknown
import org.ton.intellij.tolk.type.render
import org.ton.intellij.util.printPsi

class TolkLambdaParameterTypesProvider : AbstractTolkInlayHintProvider() {
    override fun collectFromElement(
        element: PsiElement,
        sink: InlayTreeSink,
    ) {
        if (element !is TolkLambdaFunExpression) return

        for (parameter in element.parameterList.parameterList) {
            if (parameter.typeExpression != null) continue // already has explicit type
            val parameterType = parameter.type ?: continue // no data information
            if (parameterType is TolkTyUnknown) continue   // the unknown type doesn't add any information

            sink.addPresentation(
                position = InlineInlayPosition(parameter.textRange.endOffset, false),
                hasBackground = true,
            ) {
                text(": ")
                printPsi(parameter, parameterType.render())
            }
        }
    }
}
