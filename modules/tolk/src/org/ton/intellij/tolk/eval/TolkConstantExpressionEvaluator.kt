package org.ton.intellij.tolk.eval

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.util.*
import org.ton.intellij.util.exception.ConstantEvaluationOverflowException
import org.ton.intellij.util.recursionGuard
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class TolkConstantExpressionEvaluator(
    val project: Project,
    throwOverflowException: Boolean = false
) : TolkRecursiveElementWalkingVisitor() {
    private val key = if (throwOverflowException) WITH_OVERFLOW_KEY else NO_OVERFLOW_KEY
    private val visitor = TolkConstantExpressionVisitor(project, throwOverflowException)
    private val cache get() = CachedValuesManager.getManager(project).getCachedValue(project, key, PROVIDER, false)

    override fun elementFinished(element: PsiElement) {
        if (element !is TolkExpression) return
        val value = cache[element]
        if (value == null) {
            val result = visitor.handle(element)
            if (result != null) {
                cache[element] = result
            }
        } else {
            visitor[element] = value
        }
    }

    override fun visitElement(element: PsiElement) {
        if (element !is TolkExpression) {
            super.visitElement(element)
            return
        }
        val value = cache[element]
        if (value == null) {
            super.visitElement(element)
        } else {
            visitor[element] = value
        }
    }

    companion object {
        private val PROVIDER = CachedValueProvider<ConcurrentMap<PsiElement, TolkValue>> {
            CachedValueProvider.Result.create(ConcurrentHashMap(), PsiModificationTracker.MODIFICATION_COUNT)
        }
        private val NO_OVERFLOW_KEY = Key.create<CachedValue<ConcurrentMap<PsiElement, TolkValue>>>(
            "tolk.constant_value.no_overflow"
        )
        private val WITH_OVERFLOW_KEY = Key.create<CachedValue<ConcurrentMap<PsiElement, TolkValue>>>(
            "tolk.constant_value.with_overflow"
        )

        fun compute(
            project: Project,
            expression: TolkExpression,
            throwOverflowException: Boolean = false
        ): TolkValue? {
            val evaluator = TolkConstantExpressionEvaluator(project, throwOverflowException)
            expression.accept(evaluator)
            return evaluator.cache[expression]
        }
    }
}

class TolkConstantExpressionVisitor(
    private val project: Project,
    private val throwOverflowException: Boolean = false
) : TolkVisitor() {
    private val cachedValues = mutableMapOf<TolkElement, TolkValue>()
    var result: TolkValue? = null

    fun handle(element: TolkElement): TolkValue? {
        result = null
        element.accept(this)
        return set(element, result)
    }

    operator fun set(element: TolkElement, value: TolkValue?): TolkValue? {
        if (value != null) {
            cachedValues[element] = value
        }
        return value
    }

    operator fun get(element: TolkElement?): TolkValue? {
        return cachedValues.remove(element)
    }

    override fun visitParenExpression(o: TolkParenExpression) {
        result = get(o.expression)
    }

    override fun visitTensorExpression(o: TolkTensorExpression) {
        val values = o.expressionList.map { get(it) }
        result = if (values.size == 1) {
            values.first()
        } else {
            TolkTensorValue(values)
        }
    }

    override fun visitTupleExpression(o: TolkTupleExpression) {
        result = TolkTupleValue(o.expressionList.map { get(it) })
    }

    override fun visitLiteralExpression(o: TolkLiteralExpression) {
        result = o.value
    }

    override fun visitBinExpression(o: TolkBinExpression) {
        val left = get(o.left)
        if (left == null) {
            result = null
            return
        }
        val right = get(o.right)
        if (right == null) {
            result = null
            return
        }
        if (left !is TolkIntValue || right !is TolkIntValue) {
            result = null
            return
        }
        result = compute(left, right, o.binaryOp, o, throwOverflowException)
    }

    override fun visitReferenceExpression(o: TolkReferenceExpression) {
        val resolved = o.reference?.resolve()
        result = when (resolved) {
            is TolkConstVar -> recursionGuard(resolved) {
                resolved.expression?.let {
                    TolkConstantExpressionEvaluator.compute(project, it, throwOverflowException)
                }
            }

            else -> null
        }
    }

    companion object {
        fun compute(
            left: TolkIntValue,
            right: TolkIntValue,
            op: TolkBinaryOp,
            element: TolkElement,
            throwOverflowException: Boolean = false
        ): TolkValue? {
            return try {
                when {
                    op.plus != null || op.pluslet != null -> TolkIntValue(
                        left.value.add(right.value)
                    ).checkOverflow(element, throwOverflowException)

                    op.minus != null || op.minuslet != null -> TolkIntValue(
                        left.value.subtract(right.value)
                    ).checkOverflow(element, throwOverflowException)

                    op.times != null || op.timeslet != null -> TolkIntValue(
                        left.value.multiply(right.value)
                    ).checkOverflow(element, throwOverflowException)

                    op.div != null || op.divlet != null -> {
                        if (right.value == BigInteger.ZERO) {
                            return null
                        }
                        TolkIntValue(
                            left.value.divModFloor(right.value).first
                        ).checkOverflow(element, throwOverflowException)
                    }

                    op.divc != null || op.divclet != null -> {
                        if (right.value == BigInteger.ZERO) {
                            return null
                        }
                        TolkIntValue(
                            left.value.divModCeil(right.value).first
                        ).checkOverflow(element, throwOverflowException)
                    }

                    op.divr != null || op.divrlet != null -> {
                        if (right.value == BigInteger.ZERO) {
                            return null
                        }
                        TolkIntValue(
                            left.value.divModRound(right.value).first
                        ).checkOverflow(element, throwOverflowException)
                    }

                    op.mod != null || op.modlet != null -> {
                        if (right.value == BigInteger.ZERO) {
                            return null
                        }
                        TolkIntValue(
                            left.value.divModFloor(right.value).second
                        ).checkOverflow(element, throwOverflowException)
                    }

                    op.modc != null || op.modclet != null -> {
                        if (right.value == BigInteger.ZERO) {
                            return null
                        }
                        TolkIntValue(
                            left.value.divModCeil(right.value).second
                        ).checkOverflow(element, throwOverflowException)
                    }

                    op.modr != null || op.modrlet != null -> {
                        if (right.value == BigInteger.ZERO) {
                            return null
                        }
                        TolkIntValue(
                            left.value.divModRound(right.value).second
                        ).checkOverflow(element, throwOverflowException)
                    }

                    op.and != null -> TolkIntValue(left.value.and(right.value))
                    op.or != null -> TolkIntValue(left.value.or(right.value))
                    op.xor != null -> TolkIntValue(left.value.xor(right.value))
                    op.lshift != null -> TolkIntValue(
                        left.value.shiftLeft(right.value.toInt())
                    ).checkOverflow(element, throwOverflowException)

                    op.rshift != null -> TolkIntValue(
                        left.value.shiftRight(right.value.toInt())
                    )

                    op.gt != null -> TolkIntValue(if (left.value > right.value) -BigInteger.ONE else BigInteger.ZERO)

                    op.lt != null -> TolkIntValue(
                        if (left.value < right.value) -BigInteger.ONE else BigInteger.ZERO
                    )

                    op.geq != null -> TolkIntValue(
                        if (left.value >= right.value) -BigInteger.ONE else BigInteger.ZERO
                    )

                    op.leq != null -> TolkIntValue(
                        if (left.value <= right.value) -BigInteger.ONE else BigInteger.ZERO
                    )

                    op.eq != null -> TolkIntValue(
                        if (left.value == right.value) -BigInteger.ONE else BigInteger.ZERO
                    )

                    else -> null
                }
            } catch (e: ArithmeticException) {
                if (throwOverflowException) {
                    throw ConstantEvaluationOverflowException(element)
                }
                null
            }
        }

        private fun TolkIntValue.checkOverflow(element: TolkElement, throwOverflowException: Boolean): TolkIntValue {
            if (throwOverflowException && value.bitLength() > 257) {
                throw ConstantEvaluationOverflowException(element)
            }
            return this
        }
    }
}
