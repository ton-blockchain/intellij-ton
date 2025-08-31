package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.rawReturnType
import org.ton.intellij.func.type.ty.*
import org.ton.intellij.util.parentOfType

class FuncTypeMismatchInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {

        override fun visitReturnStatement(returnStatement: FuncReturnStatement) {
            val outerFunction = returnStatement.parentOfType<FuncFunction>() ?: return
            val returnType = outerFunction.rawReturnType
            val expr = returnStatement.expression ?: return

            val inference = returnStatement.inference ?: return
            val exprType = inference.getExprTy(expr)

            if (exprType is FuncTyUnknown || returnType is FuncTyUnknown) return

            if (!returnType.canRhsBeAssigned(exprType)) {
                holder.registerProblem(
                    expr,
                    "Cannot return <code>${exprType}</code> from function with return type <code>${returnType}</code>",
                    ProblemHighlightType.GENERIC_ERROR,
                    ChangeReturnTypeFix(outerFunction, exprType.toString())
                )
            }
        }

        override fun visitBinExpression(binExpression: FuncBinExpression) {
            if (binExpression.binaryOp.eq == null) return // set op?

            val inference = binExpression.inference ?: return
            val left = binExpression.left
            val right = binExpression.right ?: return

            val leftType = inference.getExprTy(left)
            val rightType = inference.getExprTy(right)

            if (leftType is FuncTyUnknown || rightType is FuncTyUnknown) return

            if (left is FuncApplyExpression && left.left is FuncPrimitiveTypeExpression) {
                val declaredType = (left.left as FuncPrimitiveTypeExpression).rawType
                if (!declaredType.canRhsBeAssigned(rightType)) {
                    holder.registerProblem(
                        right,
                        "Cannot assign <code>${rightType}</code> to variable of type <code>${declaredType}</code>",
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
            } else if (left is FuncTensorExpression) {
                checkTensorDestructuring(left, right, rightType, holder)
            } else if (left is FuncTupleExpression) {
                checkTupleDestructuring(left, right, rightType, holder)
            } else if (left is FuncApplyExpression && left.left is FuncHoleTypeExpression && left.right is FuncTensorExpression) {
                val tensorLeft = left.right as FuncTensorExpression
                checkTensorDestructuring(tensorLeft, right, rightType, holder)
            }
        }

        override fun visitApplyExpression(applyExpression: FuncApplyExpression) {
            val inference = applyExpression.inference ?: return
            val funcRef = applyExpression.left as? FuncReferenceExpression ?: return
            val args = applyExpression.right

            val resolved = inference.getResolvedRefs(funcRef).firstOrNull()?.element as? FuncFunction ?: return

            val isMethodCall = applyExpression.parent is FuncSpecialApplyExpression

            val expectedParams = if (isMethodCall && resolved.functionParameterList.isNotEmpty()) {
                resolved.functionParameterList.subList(1, resolved.functionParameterList.size)
            } else {
                resolved.functionParameterList
            }

            if (args is FuncUnitExpression) {
                return
            }

            if (args is FuncTensorExpression) {
                val argTypes = args.expressionList.map { inference.getExprTy(it) }
                checkFunctionCall(expectedParams, argTypes, args.expressionList, holder)
            }
        }

        override fun visitTernaryExpression(ternaryExpression: FuncTernaryExpression) {
            val inference = ternaryExpression.inference ?: return
            val thenBranch = ternaryExpression.thenBranch ?: return
            val elseBranch = ternaryExpression.elseBranch ?: return

            val thenType = inference.getExprTy(thenBranch)
            val elseType = inference.getExprTy(elseBranch)

            if (thenType is FuncTyUnknown || elseType is FuncTyUnknown) return

            if (!thenType.canRhsBeAssigned(elseType) && !elseType.canRhsBeAssigned(thenType)) {
                holder.registerProblem(
                    ternaryExpression,
                    "Incompatible types in ternary expression: <code>${thenType}</code> and <code>${elseType}</code>",
                    ProblemHighlightType.GENERIC_ERROR
                )
            }
        }

        private fun checkTensorDestructuring(
            left: FuncTensorExpression,
            right: FuncExpression,
            rightType: FuncTy,
            holder: ProblemsHolder,
        ) {
            val leftVars = left.expressionList

            when (rightType) {
                is FuncTyTensor -> {
                    val rightTypes = rightType.types
                    if (leftVars.size != rightTypes.size) {
                        holder.registerProblem(
                            right,
                            "Cannot destructure tensor of ${rightTypes.size} elements into ${leftVars.size} variables",
                            ProblemHighlightType.GENERIC_ERROR
                        )
                        return
                    }

                    for (i in leftVars.indices) {
                        val variable = leftVars[i] ?: continue
                        val expectedType = rightTypes[i]

                        if (variable is FuncApplyExpression && variable.left is FuncPrimitiveTypeExpression) {
                            val declaredType = (variable.left as FuncPrimitiveTypeExpression).rawType
                            if (!declaredType.canRhsBeAssigned(expectedType)) {
                                holder.registerProblem(
                                    variable,
                                    "Cannot assign <code>${expectedType}</code> to variable of type <code>${declaredType}</code>",
                                    ProblemHighlightType.GENERIC_ERROR
                                )
                            }
                        }
                    }
                }

                is FuncTyTuple  -> {
                    val rightTypes = rightType.types
                    if (leftVars.size != rightTypes.size) {
                        holder.registerProblem(
                            right,
                            "Cannot destructure tuple of ${rightTypes.size} elements into ${leftVars.size} variables",
                            ProblemHighlightType.GENERIC_ERROR
                        )
                        return
                    }

                    for (i in leftVars.indices) {
                        val variable = leftVars[i] ?: continue
                        val expectedType = rightTypes[i]

                        if (variable is FuncApplyExpression && variable.left is FuncPrimitiveTypeExpression) {
                            val declaredType = (variable.left as FuncPrimitiveTypeExpression).rawType
                            if (!declaredType.canRhsBeAssigned(expectedType)) {
                                holder.registerProblem(
                                    variable,
                                    "Cannot assign <code>${expectedType}</code> to variable of type <code>${declaredType}</code>",
                                    ProblemHighlightType.GENERIC_ERROR
                                )
                            }
                        }
                    }
                }

                else            -> {
                    holder.registerProblem(
                        right,
                        "Cannot destructure value of type <code>${rightType}</code> into tensor variables",
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
            }
        }

        private fun checkTupleDestructuring(
            left: FuncTupleExpression,
            right: FuncExpression,
            rightType: FuncTy,
            holder: ProblemsHolder,
        ) {
            val leftVars = left.expressionList

            when (rightType) {
                is FuncTyTuple -> {
                    val rightTypes = rightType.types
                    if (leftVars.size != rightTypes.size) {
                        holder.registerProblem(
                            right,
                            "Cannot destructure tuple of ${rightTypes.size} elements into ${leftVars.size} variables",
                            ProblemHighlightType.GENERIC_ERROR
                        )
                        return
                    }

                    for (i in leftVars.indices) {
                        val variable = leftVars[i] ?: continue
                        val expectedType = rightTypes[i]

                        if (variable is FuncApplyExpression && variable.left is FuncPrimitiveTypeExpression) {
                            val declaredType = (variable.left as FuncPrimitiveTypeExpression).rawType
                            if (!declaredType.canRhsBeAssigned(expectedType)) {
                                holder.registerProblem(
                                    variable,
                                    "Cannot assign <code>${expectedType}</code> to variable of type <code>${declaredType}</code>",
                                    ProblemHighlightType.GENERIC_ERROR
                                )
                            }
                        }
                    }
                }

                else           -> {
                    holder.registerProblem(
                        right,
                        "Cannot destructure value of type <code>${rightType}</code> into tuple variables",
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
            }
        }

        private fun checkFunctionCall(
            expectedParams: List<FuncFunctionParameter>,
            argTypes: List<FuncTy>,
            argExpressions: List<FuncExpression?>,
            holder: ProblemsHolder,
        ) {
            if (expectedParams.size != argTypes.size) {
                return
            }

            for (i in expectedParams.indices) {
                val expectedType = expectedParams[i].typeReference?.rawType
                val actualType = argTypes[i]
                val argExpr = argExpressions[i]

                if (expectedType != null && argExpr != null && !expectedType.canRhsBeAssigned(actualType)) {
                    holder.registerProblem(
                        argExpr,
                        "Cannot pass <code>${actualType}</code> to parameter of type <code>${expectedType}</code>",
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
            }
        }
    }

    private class ChangeReturnTypeFix(
        function: FuncFunction,
        private val newTypeText: String,
    ) : LocalQuickFixAndIntentionActionOnPsiElement(function) {
        override fun getText(): String = "Change return type to '$newTypeText'"

        override fun getFamilyName(): String = "Change return type"

        override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
            val factory = FuncPsiFactory[project]
            val newTypeReference = factory.createTypeReference(newTypeText)
            (startElement as? FuncFunction)?.typeReference?.replace(newTypeReference)
        }
    }
}
