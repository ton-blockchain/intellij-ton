package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.*

class FuncFunctionCallInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ) = object : FuncVisitor() {
        override fun visitFunctionCallExpression(o: FuncFunctionCallExpression) {
            super.visitFunctionCallExpression(o)
            val expressionList = o.expressionList
            val function = expressionList.firstOrNull()?.reference?.resolve() as? FuncFunction ?: return
            var arguments = expressionList.getOrNull(1)?.let {
                when (it) {
                    is FuncTensorExpression -> it.expressionList
                    is FuncReferenceExpression, is FuncTupleExpression -> listOf(it)
                    else -> null
                }
            } ?: return
            val isQualified = o.dot != null || o.tilde != null
            if (isQualified) {
                val parent = o.parent
                if (parent is FuncReferenceCallExpression) {
                    arguments = listOf(parent.expressionList.first()) + arguments
                }
            }
            val parameters = function.functionParameterList
            var actualSize = arguments.size
            var expectedSize = parameters.size
            if (actualSize == expectedSize) return
            if (actualSize > expectedSize) {
                for (i in expectedSize until actualSize) {
                    tooManyArguments(arguments[i], holder)
                }
            }
            if (expectedSize > actualSize) {
                val highlightArgument =
                    if (isQualified && arguments.size == 1) expressionList.last() else arguments.last()
                for (i in actualSize until expectedSize) {
                    noValueForParameter(highlightArgument, parameters[i], holder)
                }
            }
        }
    }

    private fun tooManyArguments(element: PsiElement, holder: ProblemsHolder) {
        holder.registerProblem(element, "Too many arguments")
    }

    private fun noValueForParameter(element: PsiElement, parameter: FuncFunctionParameter, holder: ProblemsHolder) {
        val name = parameter.name
        if (name != null) {
            holder.registerProblem(element, "No value for parameter `$name`")
        } else {
            holder.registerProblem(element, "Not enough arguments")
        }
    }
}
