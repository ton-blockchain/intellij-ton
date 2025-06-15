package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.util.nextOrNull

internal fun iterateOverParameters(
    callExpression: TolkCallExpression,
    referenceResolver: (TolkReferenceElement) -> PsiElement? = { it.reference?.resolve() },
    processor: (TolkParameterElement, TolkArgument?) -> Unit
) {
    val calleeExpression = callExpression.expression
    val argumentIterator = callExpression.argumentList.argumentList.iterator()
    val parameterIterator = when (calleeExpression) {
        is TolkReferenceExpression -> {
            val resolvedFunction = referenceResolver(calleeExpression) as? TolkFunction ?: return
            val parameterList = resolvedFunction.parameterList ?: return
            parameterList.parameterList.iterator()
        }
        is TolkDotExpression -> {
            val resolvedMethod = calleeExpression.fieldLookup?.let(referenceResolver) as? TolkFunction ?: return
            val parameterList = resolvedMethod.parameterList ?: return
            val receiver = calleeExpression.expression
            // empty references means its reference to primitive type
            val selfParameter = parameterList.selfParameter
            if (selfParameter != null
                && receiver is TolkReferenceExpression
                && (receiver.references.isEmpty() || referenceResolver(receiver) is TolkTypeSymbolElement)
            ) {
                val selfArg = argumentIterator.nextOrNull()
                if (selfArg != null) {
                    processor(selfParameter, selfArg)
                }
            }
            parameterList.parameterList.iterator()
        }
        else -> return
    }

    while (parameterIterator.hasNext()) {
        val parameter = parameterIterator.next()
        val argument = argumentIterator.nextOrNull()
        processor(parameter, argument)
    }
}
