package org.ton.intellij.tact.eval

import org.ton.intellij.tact.psi.*
import org.ton.intellij.tvm.math.divModFloor
import org.ton.intellij.util.recursionGuard
import java.math.BigInteger

fun TactExpression.evaluate(): TactValue? = TactEvaluator.evaluate(this)

object TactEvaluator {
    fun evaluate(expression: TactExpression): TactValue? {
        return when (expression) {
            is TactIntegerExpression -> expression.eval()
            is TactUnaryExpression -> expression.eval()
            is TactBinExpression -> expression.eval()
            is TactReferenceExpression -> expression.eval()
            else -> null
        }
    }

    @Suppress("HardCodedStringLiteral")
    private fun TactIntegerExpression.eval(): TactValue? {
        val text = integerLiteral.text.replace("_", "")
        return try {
            if (text.startsWith("0x") || text.startsWith("0X")) {
                TactIntValue(BigInteger(text.substring(2), 16))
            } else if (text.startsWith("0b") || text.startsWith("0B")) {
                TactIntValue(BigInteger(text.substring(2), 2))
            } else if (text.startsWith("0o") || text.startsWith("0O")) {
                TactIntValue(BigInteger(text.substring(2), 8))
            } else {
                TactIntValue(BigInteger(text))
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun TactUnaryExpression.eval(): TactValue? {
        val value = expression?.evaluate() ?: return null
        when (value) {
            is TactIntValue -> when {
                plus != null -> return value
                minus != null -> return TactIntValue(value.value.negate())
                tilde != null -> return TactIntValue(value.value.not())
            }

            is TactBoolValue -> if (excl != null) {
                return TactBoolValue(!value.value)
            }

            else -> {}
        }
        return null
    }

    private fun TactBinExpression.eval(): TactValue? {
        val rightValue = right?.evaluate() ?: return null
        val leftValue = left.evaluate() ?: return null
        val op = binOp
        when {
            op.plus != null -> {
                if (leftValue is TactIntValue && rightValue is TactIntValue) {
                    return TactIntValue(leftValue.value + rightValue.value)
                }
            }

            op.minus != null -> {
                if (leftValue is TactIntValue && rightValue is TactIntValue) {
                    return TactIntValue(leftValue.value - rightValue.value)
                }
            }

            op.mul != null -> {
                if (leftValue is TactIntValue && rightValue is TactIntValue) {
                    return TactIntValue(leftValue.value * rightValue.value)
                }
            }

            // The semantics of integer division for TVM (and by extension in Tact)
            // is a non-conventional one: by default it rounds towards negative infinity,
            // meaning, for instance, -1 / 5 = -1 and not zero, as in many mainstream languages.
            // Still, the following holds: a / b * b + a % b == a, for all b != 0.
            op.div != null -> {
                val r = (rightValue as? TactIntValue)?.value ?: return null
                if (r == BigInteger.ZERO) {
                    return null
                }
                val d = (leftValue as? TactIntValue)?.value ?: return null
                return TactIntValue(d.divModFloor(r).first)
            }

            op.rem != null -> {
                val r = (rightValue as? TactIntValue)?.value ?: return null
                if (r == BigInteger.ZERO) {
                    return null
                }
                val d = (leftValue as? TactIntValue)?.value ?: return null
                return TactIntValue(d.divModFloor(r).second)
            }

            op.and != null -> {
                if (leftValue is TactIntValue && rightValue is TactIntValue) {
                    return TactIntValue(leftValue.value and rightValue.value)
                }
            }

            op.or != null -> {
                if (leftValue is TactIntValue && rightValue is TactIntValue) {
                    return TactIntValue(leftValue.value or rightValue.value)
                }
            }

            op.xor != null -> {
                if (leftValue is TactIntValue && rightValue is TactIntValue) {
                    return TactIntValue(leftValue.value xor rightValue.value)
                }
            }

            op.ltlt != null -> {
                if (leftValue is TactIntValue && rightValue is TactIntValue) {
                    return TactIntValue(leftValue.value shl rightValue.value.toInt())
                }
            }

            op.gtgt != null -> {
                if (leftValue is TactIntValue && rightValue is TactIntValue) {
                    return TactIntValue(leftValue.value shr rightValue.value.toInt())
                }
            }

            op.gt != null -> {
                if (leftValue is TactIntValue && rightValue is TactIntValue) {
                    return TactBoolValue(leftValue.value > rightValue.value)
                }
            }

            op.lt != null -> {
                if (leftValue is TactIntValue && rightValue is TactIntValue) {
                    return TactBoolValue(leftValue.value < rightValue.value)
                }
            }

            op.gteq != null -> {
                if (leftValue is TactIntValue && rightValue is TactIntValue) {
                    return TactBoolValue(leftValue.value >= rightValue.value)
                }
            }

            op.lteq != null -> {
                if (leftValue is TactIntValue && rightValue is TactIntValue) {
                    return TactBoolValue(leftValue.value <= rightValue.value)
                }
            }

            op.eqeq != null -> {
                return TactBoolValue(leftValue == rightValue)
            }

            op.excleq != null -> {
                return TactBoolValue(leftValue != rightValue)
            }

            op.andand != null -> {
                if (leftValue is TactBoolValue && rightValue is TactBoolValue) {
                    return TactBoolValue(leftValue.value && rightValue.value)
                }
            }

            op.oror != null -> {
                if (leftValue is TactBoolValue && rightValue is TactBoolValue) {
                    return TactBoolValue(leftValue.value || rightValue.value)
                }
            }
        }
        return null
    }

    private fun TactReferenceExpression.eval(): TactValue? = recursionGuard(this, memoize = false) {
        val resolved = reference?.resolve() ?: return@recursionGuard null
        when (resolved) {
            is TactLetStatement -> resolved.expression?.evaluate()
            is TactConstant -> resolved.expression?.evaluate()
            else -> null
        }
    }
}
