package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.*

// TODO fix
class TolkFunctionCallInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ) = object : TolkVisitor() {
        override fun visitSpecialApplyExpression(o: TolkSpecialApplyExpression) {
//            super.visitSpecialApplyExpression(o)
//            val function = o.left.reference?.resolve() as? TolkFunction ?: return
//            holder.check(function, o.right ?: return, true)
        }

//        override fun visitApplyExpression(o: TolkApplyExpression) {
//            super.visitApplyExpression(o)
//            if (o.parent is TolkSpecialApplyExpression) return
//            val function = o.left.reference?.resolve() as? TolkFunction ?: return
//            holder.check(function, o.right ?: return, false)
//        }
    }

    private fun ProblemsHolder.check(function: TolkFunction, argument: TolkExpression, isMethodCall: Boolean) {
        val arguments = argument.let {
            when (it) {
                is TolkTensorExpression -> it.expressionList
                is TolkUnitExpression -> emptyList()
                else -> listOf(it)
            }
        }
        val parameters = function.functionParameterList

        var actualSize = arguments.size
        // if parameters.size - 1 == -1, this is situation if try call a.foo(), when foo() doesn't have parameters
        var expectedSize = if (isMethodCall) {
            if (parameters.size == 0) {
                tooManyArguments(argument)
                return
            }
            parameters.size - 1
        } else {
            parameters.size
        }
        if (actualSize == expectedSize) return
        if (actualSize > expectedSize) {
            for (i in expectedSize until actualSize) {
                tooManyArguments(arguments[i])
            }
        }
        if (expectedSize > actualSize) {
            val highlightArgument = arguments.lastOrNull() ?: argument
            expectedSize += if (isMethodCall) 1 else 0
            actualSize += if (isMethodCall) 1 else 0
            for (i in actualSize until expectedSize) {
                noValueForParameter(highlightArgument, parameters[i])
            }
        }
    }

    private fun ProblemsHolder.tooManyArguments(element: PsiElement) {
        registerProblem(element, "Too many arguments")
    }

    private fun ProblemsHolder.noValueForParameter(element: PsiElement, parameter: TolkFunctionParameter) {
        val name = parameter.name
        if (name != null) {
            registerProblem(element, "No value for parameter `$name`")
        } else {
            registerProblem(element, "Not enough arguments")
        }
    }
}
