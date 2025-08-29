package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tolk.psi.TolkBinExpression
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkReturnStatement
import org.ton.intellij.tolk.psi.TolkStructExpressionField
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.tolk.psi.TolkVar
import org.ton.intellij.tolk.psi.TolkVarDefinition
import org.ton.intellij.tolk.psi.TolkVarExpression
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.psi.impl.functionSymbol
import org.ton.intellij.tolk.psi.impl.isSetAssignment
import org.ton.intellij.tolk.psi.impl.returnTy
import org.ton.intellij.tolk.psi.unwrapParentheses
import org.ton.intellij.tolk.type.inference
import org.ton.intellij.tolk.type.render
import org.ton.intellij.util.parentOfType

class TolkTypeMismatchInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitVarExpression(decl: TolkVarExpression) {
            val def = decl.varDefinition
            if (def !is TolkVar) return
            if (def.typeExpression == null) return // no explicit type hint

            val defType = def.type ?: return
            val expression = decl.expression ?: return
            val exprType = expression.type ?: return

            if (!defType.canRhsBeAssigned(exprType)) {
                holder.registerProblem(
                    expression,
                    "Cannot assign <code>${exprType.render()}</code> to variable of type <code>${defType.render()}</code>",
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }

        override fun visitBinExpression(bin: TolkBinExpression) {
            if (bin.binaryOp.eq != null || bin.isSetAssignment) {
                checkAssignment(bin)
            }
        }

        override fun visitStructExpressionField(init: TolkStructExpressionField) {
            val fieldType = init.inference?.getType(init) ?: return

            val expression = init.expression ?: return
            val exprType = expression.type ?: return

            if (!fieldType.canRhsBeAssigned(exprType)) {
                holder.registerProblem(
                    expression,
                    "Cannot assign <code>${exprType.render()}</code> to field of type <code>${fieldType.render()}</code>",
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }

        override fun visitStructField(field: TolkStructField) {
            val fieldType = field.type ?: return
            val defaultExpr = field.expression ?: return
            val defaultExprType = defaultExpr.type ?: return

            if (!fieldType.canRhsBeAssigned(defaultExprType)) {
                holder.registerProblem(
                    defaultExpr,
                    "Cannot assign <code>${defaultExprType.render()}</code> to field of type <code>${fieldType.render()}</code>",
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }

        override fun visitCallExpression(call: TolkCallExpression) {
            val called = call.functionSymbol ?: return
            val instanceMethodCall = isInstanceMethodCall(call)
            val args = call.argumentList.argumentList
            val selfParam = called.parameterList?.selfParameter
            val rawParams = called.parameterList?.parameterList ?: emptyList()
            val params = if (selfParam != null && !instanceMethodCall) listOf(selfParam, *rawParams.toTypedArray()) else rawParams

            args.forEachIndexed { i, arg ->
                val argType = arg.expression.type ?: return@forEachIndexed
                val param = params.getOrNull(i) ?: return@forEachIndexed
                val paramType = param.type ?: return@forEachIndexed

                if (!paramType.canRhsBeAssigned(argType)) {
                    holder.registerProblem(
                        arg,
                        "Cannot pass <code>${argType.render()}</code> to parameter of type <code>${paramType.render()}</code>",
                        ProblemHighlightType.GENERIC_ERROR,
                    )
                }
            }
        }

        override fun visitReturnStatement(returnStatement: TolkReturnStatement) {
            val outerFunction = returnStatement.parentOfType<TolkFunction>() ?: return
            if (outerFunction.returnType == null) {
                // no explicit return type, don't check
                return
            }

            val returnType = outerFunction.returnTy
            val expr = returnStatement.expression ?: return
            val exprType = expr.type ?: return

            if (!returnType.canRhsBeAssigned(exprType)) {
                holder.registerProblem(
                    expr,
                    "Cannot return <code>${exprType.render()}</code> from function with return type <code>${returnType.render()}</code>",
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }

        private fun checkAssignment(bin: TolkBinExpression) {
            val left = bin.left.unwrapParentheses() ?: return
            val right = bin.right ?: return
            val leftType = left.type ?: return
            val rightType = right.type ?: return

            if (!leftType.canRhsBeAssigned(rightType)) {
                val assignTarget = findAssignTarget(left) ?: left
                val target = when (assignTarget.reference?.resolve()) {
                    is TolkVarDefinition -> "variable of type <code>${leftType.render()}</code>"
                    is TolkStructField   -> "field of type <code>${leftType.render()}</code>"
                    else                 -> "<code>${leftType.render()}</code>"
                }

                holder.registerProblem(
                    right,
                    "Cannot assign <code>${rightType.render()}</code> to $target",
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }

        private fun findAssignTarget(left: TolkExpression): TolkElement? {
            if (left is TolkDotExpression) {
                return left.fieldLookup
            }
            return left
        }
    }
}
