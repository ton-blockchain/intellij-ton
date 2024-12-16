package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.tolk.psi.TolkVisitor

class TolkUnusedFunctionParameterInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitFunction(o: TolkFunction) {
            o.functionBody?.blockStatement ?: return
            val parameters = o.parameterList?.parameterList ?: return
            val searchScope = LocalSearchScope(o)
            for (parameter in parameters) {
                ProgressIndicatorProvider.checkCanceled()
                processParameter(parameter, searchScope)
            }
        }

        private fun processParameter(parameter: TolkParameter, searchScope: SearchScope) {
            val id = parameter.identifier
            if (id.textMatches("_")) return
            if (ReferencesSearch.search(parameter, searchScope).none()) {
                holder.registerProblem(
                    parameter,
                    "Unused parameter <code>#ref</code> #loc",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    id.textRangeInParent
                )
            }
        }
    }
}
