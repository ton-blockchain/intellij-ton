package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkEnum
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypeParameter
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.psi.impl.functionSymbol
import org.ton.intellij.tolk.psi.impl.hasSelf

class TolkCallArgumentsCountMismatchInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitCallExpression(call: TolkCallExpression) {
            val called = call.functionSymbol ?: return
            checkCall(call, called)
        }

        private fun checkCall(call: TolkCallExpression, called: TolkFunction) {
            val deltaSelf = if (isInstanceMethodCall(call) && called.hasSelf) 1 else 0
            val args = call.argumentList.argumentList
            val argsCount = args.size + deltaSelf
            val selfParam = called.parameterList?.selfParameter
            val rawParams = called.parameterList?.parameterList ?: emptyList()
            val params = if (selfParam != null) listOf(selfParam, *rawParams.toTypedArray()) else rawParams
            val maxParams = params.size

            var minParams = maxParams
            while (minParams > 0 && (params[minParams - 1] as? TolkParameter)?.parameterDefault != null) {
                minParams--
            }

            if (argsCount > maxParams) {
                // foo(1, 2, 3)
                //        ^^^^ here
                val startIndex = (maxParams - deltaSelf).coerceAtLeast(0)
                val extraArgs = args.subList(startIndex, args.size)
                val startPosition = (extraArgs.getOrNull(0) ?: call).startOffsetInParent
                val lastExtraArg = extraArgs.getOrNull(extraArgs.size - 1) ?: call
                val endPosition = lastExtraArg.startOffsetInParent + lastExtraArg.textLength

                val descriptor = holder.manager.createProblemDescriptor(
                    call.argumentList,
                    if (startIndex == 0 && args.isEmpty()) null else TextRange(startPosition, endPosition),
                    "Too many arguments in call to <code>${called.name}</code>, expected ${maxParams - deltaSelf}, have ${argsCount - deltaSelf}",
                    ProblemHighlightType.GENERIC_ERROR,
                    false
                )

                holder.registerProblem(descriptor)
            }

            if (argsCount < minParams) {
                // foo()
                //     ^ here
                val anchor = call.argumentList.lastChild
                holder.registerProblem(
                    anchor,
                    "Too few arguments in call to <code>${called.name}</code>, expected ${maxParams - deltaSelf}, have ${argsCount - deltaSelf}",
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }
    }
}

fun isInstanceMethodCall(call: TolkCallExpression): Boolean {
    val qualifier = getQualifier(call.expression) ?: return false
    if (qualifier is TolkReferenceExpression) {
        if ((qualifier.typeArgumentList?.typeExpressionList?.size ?: 0) > 0) {
            // Foo<int>.bar()
            return false
        }

        val resolved = qualifier.reference?.resolve()
        if (resolved is TolkStruct || resolved is TolkTypeDef || resolved is TolkTypeParameter || resolved is TolkEnum) {
            // likely Foo.bar() static call
            return false
        }
        if (resolved == null) {
            // likely int8.bar() with old Tolk stdlib
            return false
        }

        // instance method call like foo.bar()
        return true
    }

    // instance method call like foo.bar()
    return true
}

private fun getQualifier(expression: TolkExpression): TolkElement? {
    if (expression is TolkReferenceExpression) {
        return null
    }
    if (expression is TolkDotExpression) {
        return expression.expression
    }
    return null
}
