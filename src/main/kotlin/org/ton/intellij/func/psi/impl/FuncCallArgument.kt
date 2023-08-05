package org.ton.intellij.func.psi.impl

import org.ton.intellij.func.psi.FuncCallArgument
import org.ton.intellij.func.psi.FuncExpression
import org.ton.intellij.func.psi.FuncTupleExpression

fun FuncCallArgument.collectArguments(): List<FuncExpression> {
    val expression = expression
    return if (expression is FuncTupleExpression) {
        expression.expressionList
    } else {
        listOf(expression)
    }
}
