package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.rBrace

class TolkMissingReturnInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): TolkVisitor =
        object : TolkVisitor() {
            override fun visitFunction(o: TolkFunction) {
                check(o, holder)
            }
        }

    private fun check(function: TolkFunction, holder: ProblemsHolder) {
        // TODO: fix after type system is implemented
//        val block = function.blockStatement ?: return
//        val atomicType = function.typeReference as? TolkAtomicType ?: return
//        val isVoid = atomicType is TolkHoleType || (atomicType as? TolkTensorType)?.typeReferenceList?.isEmpty() == true
//        if (isVoid) return
//        holder.isTerminating(block)
    }

    private fun ProblemsHolder.isTerminating(element: TolkElement?): Boolean {
        ProgressIndicatorProvider.checkCanceled()
        return when (element) {
            null -> false
            is TolkReturnStatement -> true
            is TolkBlockStatement -> {
                val result = isTerminating(element.statementList.lastOrNull())
                if (!result) {
                    val brace = element.rBrace
                    registerProblem(brace ?: element, "Missing `return`")
                }
                result
            }

            is TolkIfStatement -> isTerminating(element.blockStatement) && isTerminating(element.elseBranch)
            is TolkElseBranch -> isTerminating(element.statement)
            is TolkTryStatement -> isTerminating(element.blockStatement) && isTerminating(element.catch)
            is TolkCatch -> isTerminating(element.blockStatement)
            else -> false
        }
    }
}
