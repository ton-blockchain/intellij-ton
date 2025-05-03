package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.hasSelf

class TolkParameterHintsProvider : AbstractTolkInlayHintProvider() {
    override fun collectFromElement(
        element: PsiElement,
        sink: InlayTreeSink
    ) {
        if (element !is TolkArgumentList) return
        val callExpression = element.parentOfType<TolkCallExpression>() ?: return
        val callee = callExpression.expression
        val function = when (callee) {
            is TolkReferenceExpression -> {
                callee.reference?.resolve() as? TolkFunction
            }
            is TolkDotExpression -> {
                callee.right?.reference?.resolve() as? TolkFunction
            }
            else -> null
        } ?: return

        val parameterIterator =  function.parameterList?.parameterList?.iterator() ?: return
        if (callee is TolkDotExpression && !function.hasSelf && parameterIterator.hasNext()) {
            parameterIterator.next()
        }
        val argumentList = element.argumentList.iterator()
        while (parameterIterator.hasNext() && argumentList.hasNext()) {
            val parameter = parameterIterator.next()
            val argument = argumentList.next()
            val parameterName = parameter.name ?: continue
            if (parameterName.isEmpty() || parameterName == "_" || parameterName == "self") continue
            val argumentName = (argument.expression as? TolkReferenceExpression)?.name
            if (argumentName == parameterName) continue

            sink.addPresentation(
                position = InlineInlayPosition(argument.textRange.startOffset,  false),
                hasBackground = true,
            ) {
                text("$parameterName =")
            }
        }
    }
}
