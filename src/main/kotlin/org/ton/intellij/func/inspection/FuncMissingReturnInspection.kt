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
        // TODO: fix after type system is implemented
//        val block = function.blockStatement ?: return
//        val atomicType = function.typeReference as? FuncAtomicType ?: return
//        val isVoid = atomicType is FuncHoleType || (atomicType as? FuncTensorType)?.typeReferenceList?.isEmpty() == true
//        if (isVoid) return
//        holder.isTerminating(block)
    }

    private fun ProblemsHolder.isTerminating(element: FuncElement?): Boolean {
        ProgressIndicatorProvider.checkCanceled()
        return when (element) {
            null -> false
            is FuncReturnStatement -> true
            is FuncBlockStatement -> {
                val result = isTerminating(element.statementList.lastOrNull())
                if (!result) {
                    val brace = element.rBrace
                    registerProblem(brace ?: element, "Missing `return`")
                }
                result
            }
            is FuncIfStatement -> isTerminating(element.blockStatement) && isTerminating(
                element.elseBranch ?: element.elseIfBranch
            )
            is FuncElseBranch -> isTerminating(element.blockStatement)
            is FuncElseIfBranch ->
                isTerminating(element.blockStatement) && isTerminating(element.elseBranch ?: element.elseIfBranch)
            is FuncTryStatement -> isTerminating(element.blockStatement) && isTerminating(element.catch)
            is FuncCatch -> isTerminating(element.blockStatement)
            else -> false
        }
    }
}
