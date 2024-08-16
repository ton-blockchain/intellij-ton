package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkFunctionParameter
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.psi.impl.hasAsm

class TolkUnusedFunctionParameterInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitFunction(o: TolkFunction) {
            if (o.hasAsm) return
            val parameters = o.functionParameterList
            for (parameter in parameters) {
                ProgressIndicatorProvider.checkCanceled()
                processParameter(parameter)
            }
        }

        private fun processParameter(parameter: TolkFunctionParameter) {
            val id = parameter.identifier ?: return
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
