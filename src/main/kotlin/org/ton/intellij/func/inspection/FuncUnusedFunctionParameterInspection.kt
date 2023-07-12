package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncFunctionParameter
import org.ton.intellij.func.psi.FuncVisitor
import org.ton.intellij.func.psi.impl.hasAsm

class FuncUnusedFunctionParameterInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitFunction(o: FuncFunction) {
            if (o.hasAsm) return
            val parameters = o.functionParameterList?.functionParameterList ?: return
            for (parameter in parameters) {
                ProgressIndicatorProvider.checkCanceled()
                processParameter(parameter)
            }
        }

        private fun processParameter(parameter: FuncFunctionParameter) {
            val id = parameter.identifier
            if (ReferencesSearch.search(parameter, parameter.useScope).findFirst() == null) {
                val range = TextRange.from(id.startOffsetInParent, id.textLength)
                holder.registerProblem(
                    parameter,
                    "Unused parameter <code>#ref</code> #loc",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    range
                )
            }
        }
    }
}
