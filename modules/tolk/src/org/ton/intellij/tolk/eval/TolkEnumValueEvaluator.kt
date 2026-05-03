package org.ton.intellij.tolk.eval

import org.ton.intellij.tolk.psi.TolkEnum
import org.ton.intellij.tolk.psi.TolkEnumMember
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.impl.members
import org.ton.intellij.tolk.psi.impl.parentEnum
import org.ton.intellij.util.recursionGuard
import java.math.BigInteger

object TolkEnumValueEvaluator {
    private val START_VALUE = BigInteger.valueOf(-1)
    private val STEP = BigInteger.ONE

    fun compute(member: TolkEnumMember): BigInteger? = computeValues(member.parentEnum)[member]

    fun computeValues(enum: TolkEnum): Map<TolkEnumMember, BigInteger> = recursionGuard(enum, memoize = false) {
        computeValuesInner(enum)
    }.orEmpty()

    private fun computeValuesInner(enum: TolkEnum): Map<TolkEnumMember, BigInteger> {
        val values = linkedMapOf<TolkEnumMember, BigInteger>()
        var previousValue: BigInteger? = START_VALUE

        for (member in enum.members) {
            val expression = member.expression
            val value = if (expression != null) {
                computeInitializer(expression)
            } else {
                previousValue?.add(STEP)
            }

            if (value != null) {
                values[member] = value
            }
            previousValue = value
        }

        return values
    }

    private fun computeInitializer(expression: TolkExpression): BigInteger? {
        val value = TolkConstantExpressionEvaluator.compute(expression.project, expression)
        return (value as? TolkIntValue)?.value
    }
}
