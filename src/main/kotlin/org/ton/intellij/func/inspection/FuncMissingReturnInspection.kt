package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.rBrace

class FuncMissingReturnInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): FuncVisitor =
        object : FuncVisitor() {
            override fun visitFunction(o: FuncFunction) {
                check(o, holder)
            }
        }

    private fun check(function: FuncFunction, holder: ProblemsHolder) {
        val block = function.blockStatement ?: return
        val atomicType = function.type as? FuncAtomicType ?: return
        val isVoid = atomicType is FuncHoleType || (atomicType as? FuncTensorType)?.typeList?.isEmpty() == true
        if (isVoid || isTerminating(block)) return
        val brace = block.rBrace
        holder.registerProblem(brace ?: block, "Missing return at end of function")
    }

    private fun isTerminating(element: FuncElement?): Boolean {
        ProgressIndicatorProvider.checkCanceled()
        return when (element) {
            null -> false
            is FuncReturnStatement -> true
            is FuncBlockStatement -> isTerminating(element.statementList.lastOrNull())
            is FuncIfStatement -> isTerminating(element.blockStatement) && isTerminating(
                element.`else` ?: element.elseIf
            )

            is FuncElse -> isTerminating(element.blockStatement)
            is FuncElseIf ->
                isTerminating(element.blockStatement) && isTerminating(element.`else` ?: element.elseIf)

            else -> false
        }
    }
}
