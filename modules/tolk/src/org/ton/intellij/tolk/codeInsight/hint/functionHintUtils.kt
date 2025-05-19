package org.ton.intellij.tolk.codeInsight.hint

import org.ton.intellij.tolk.psi.*
import org.ton.intellij.util.nextOrNull

internal fun iterateOverParameters(
    callExpression: TolkCallExpression,
    processor: (TolkParameterElement, TolkArgument) -> Unit
) {
    val calleeExpression = callExpression.expression
    val argumentIterator = callExpression.argumentList.argumentList.iterator()
    val parameterIterator = when (calleeExpression) {
        is TolkReferenceExpression -> {
            val resolvedFunction = calleeExpression.reference?.resolve() as? TolkFunction ?: return
            val parameterList = resolvedFunction.parameterList ?: return
            parameterList.parameterList.iterator()
        }
        is TolkDotExpression -> {
            val resolvedMethod = calleeExpression.fieldLookup?.reference?.resolve() as? TolkFunction ?: return
            val parameterList = resolvedMethod.parameterList ?: return
            val receiver = calleeExpression.expression
            // empty references means its reference to primitive type
            val selfParameter = parameterList.selfParameter
            if (selfParameter != null
                && receiver is TolkReferenceExpression
                && (receiver.references.isEmpty() || receiver.reference?.resolve() is TolkTypeSymbolElement)
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

    while (parameterIterator.hasNext() && argumentIterator.hasNext()) {
        val parameter = parameterIterator.next()
        val argument = argumentIterator.next()
        processor(parameter, argument)
    }
}
