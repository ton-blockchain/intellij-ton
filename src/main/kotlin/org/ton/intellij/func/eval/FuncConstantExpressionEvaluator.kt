package org.ton.intellij.func.eval

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.ton.intellij.func.psi.*
import org.ton.intellij.tvm.math.divModCeil
import org.ton.intellij.tvm.math.divModFloor
import org.ton.intellij.tvm.math.divModRound
import org.ton.intellij.util.exception.ConstantEvaluationOverflowException
import org.ton.intellij.util.recursionGuard
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class FuncConstantExpressionEvaluator(
    val project: Project,
    throwOverflowException: Boolean = false
) : FuncRecursiveElementWalkingVisitor() {
    private val key = if (throwOverflowException) WITH_OVERFLOW_KEY else NO_OVERFLOW_KEY
    private val visitor = FuncConstantExpressionVisitor(project, throwOverflowException)
    private val cache get() = CachedValuesManager.getManager(project).getCachedValue(project, key, PROVIDER, false)

    override fun elementFinished(element: PsiElement) {
        if (element !is FuncExpression) return
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
        if (element !is FuncExpression) {
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
        private val PROVIDER = CachedValueProvider<ConcurrentMap<PsiElement, FuncValue>> {
            CachedValueProvider.Result.create(ConcurrentHashMap(), PsiModificationTracker.MODIFICATION_COUNT)
        }
        private val NO_OVERFLOW_KEY = Key.create<CachedValue<ConcurrentMap<PsiElement, FuncValue>>>(
            "func.constant_value.no_overflow"
        )
        private val WITH_OVERFLOW_KEY = Key.create<CachedValue<ConcurrentMap<PsiElement, FuncValue>>>(
            "func.constant_value.with_overflow"
        )

        fun compute(
            project: Project,
            expression: FuncExpression,
            throwOverflowException: Boolean = false
        ): FuncValue? {
            val evaluator = FuncConstantExpressionEvaluator(project, throwOverflowException)
            expression.accept(evaluator)
            return evaluator.cache[expression]
        }
    }
}

class FuncConstantExpressionVisitor(
    private val project: Project,
    private val throwOverflowException: Boolean = false
) : FuncVisitor() {
    private val cachedValues = mutableMapOf<FuncElement, FuncValue>()
    var result: FuncValue? = null

    fun handle(element: FuncElement): FuncValue? {
        result = null
        element.accept(this)
        return set(element, result)
    }

    operator fun set(element: FuncElement, value: FuncValue?): FuncValue? {
        if (value != null) {
            cachedValues[element] = value
        }
        return value
    }

    operator fun get(element: FuncElement?): FuncValue? {
        return cachedValues.remove(element)
    }

    override fun visitParenExpression(o: FuncParenExpression) {
        result = get(o.expression)
    }

    override fun visitTensorExpression(o: FuncTensorExpression) {
        val values = o.expressionList.map { get(it) }
        result = if (values.size == 1) {
            values.first()
        } else {
            FuncTensorValue(values)
        }
    }

    override fun visitTupleExpression(o: FuncTupleExpression) {
        result = FuncTupleValue(o.expressionList.map { get(it) })
    }

    override fun visitLiteralExpression(o: FuncLiteralExpression) {
        result = o.value
    }

    override fun visitBinExpression(o: FuncBinExpression) {
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
        if (left !is FuncIntValue || right !is FuncIntValue) {
            result = null
            return
        }
        result = compute(left, right, o.binaryOp, o, throwOverflowException)
    }

    override fun visitReferenceExpression(o: FuncReferenceExpression) {
        val resolved = o.reference?.resolve()
        result = when (resolved) {
            is FuncConstVar -> recursionGuard(resolved) {
                resolved.expression?.let {
                    FuncConstantExpressionEvaluator.compute(project, it, throwOverflowException)
                }
            }

            else -> null
        }
    }

    companion object {
        fun compute(
            left: FuncIntValue,
            right: FuncIntValue,
            op: FuncBinaryOp,
            element: FuncElement,
            throwOverflowException: Boolean = false
        ): FuncValue? {
            return try {
                when {
                    op.plus != null || op.pluslet != null -> FuncIntValue(
                        left.value.add(right.value)
                    ).checkOverflow(element, throwOverflowException)

                    op.minus != null || op.minuslet != null -> FuncIntValue(
                        left.value.subtract(right.value)
                    ).checkOverflow(element, throwOverflowException)

                    op.times != null || op.timeslet != null -> FuncIntValue(
                        left.value.multiply(right.value)
                    ).checkOverflow(element, throwOverflowException)

                    op.div != null || op.divlet != null -> {
                        if (right.value == BigInteger.ZERO) {
                            return null
                        }
                        FuncIntValue(
                            left.value.divModFloor(right.value).first
                        ).checkOverflow(element, throwOverflowException)
                    }

                    op.divc != null || op.divclet != null -> {
                        if (right.value == BigInteger.ZERO) {
                            return null
                        }
                        FuncIntValue(
                            left.value.divModCeil(right.value).first
                        ).checkOverflow(element, throwOverflowException)
                    }

                    op.divr != null || op.divrlet != null -> {
                        if (right.value == BigInteger.ZERO) {
                            return null
                        }
                        FuncIntValue(
                            left.value.divModRound(right.value).first
                        ).checkOverflow(element, throwOverflowException)
                    }

                    op.mod != null || op.modlet != null -> {
                        if (right.value == BigInteger.ZERO) {
                            return null
                        }
                        FuncIntValue(
                            left.value.divModFloor(right.value).second
                        ).checkOverflow(element, throwOverflowException)
                    }

                    op.modc != null || op.modclet != null -> {
                        if (right.value == BigInteger.ZERO) {
                            return null
                        }
                        FuncIntValue(
                            left.value.divModCeil(right.value).second
                        ).checkOverflow(element, throwOverflowException)
                    }

                    op.modr != null || op.modrlet != null -> {
                        if (right.value == BigInteger.ZERO) {
                            return null
                        }
                        FuncIntValue(
                            left.value.divModRound(right.value).second
                        ).checkOverflow(element, throwOverflowException)
                    }

                    op.and != null -> FuncIntValue(left.value.and(right.value))
                    op.or != null -> FuncIntValue(left.value.or(right.value))
                    op.xor != null -> FuncIntValue(left.value.xor(right.value))
                    op.lshift != null -> FuncIntValue(
                        left.value.shiftLeft(right.value.toInt())
                    ).checkOverflow(element, throwOverflowException)

                    op.rshift != null -> FuncIntValue(
                        left.value.shiftRight(right.value.toInt())
                    )

                    op.gt != null -> FuncIntValue(if (left.value > right.value) -BigInteger.ONE else BigInteger.ZERO)

                    op.lt != null -> FuncIntValue(
                        if (left.value < right.value) -BigInteger.ONE else BigInteger.ZERO
                    )

                    op.geq != null -> FuncIntValue(
                        if (left.value >= right.value) -BigInteger.ONE else BigInteger.ZERO
                    )

                    op.leq != null -> FuncIntValue(
                        if (left.value <= right.value) -BigInteger.ONE else BigInteger.ZERO
                    )

                    op.eq != null -> FuncIntValue(
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

        private fun FuncIntValue.checkOverflow(element: FuncElement, throwOverflowException: Boolean): FuncIntValue {
            if (throwOverflowException && value.bitLength() > 257) {
                throw ConstantEvaluationOverflowException(element)
            }
            return this
        }
    }
}
