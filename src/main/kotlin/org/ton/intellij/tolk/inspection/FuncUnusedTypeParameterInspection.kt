package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkTypeParameter
import org.ton.intellij.tolk.psi.TolkVisitor

class TolkUnusedTypeParameterInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitFunction(o: TolkFunction) {
            val parameters = o.typeParameterList
            for (parameter in parameters) {
                ProgressIndicatorProvider.checkCanceled()
                processParameter(parameter)
            }
        }

        private fun processParameter(parameter: TolkTypeParameter) {
            if (ReferencesSearch.search(parameter).any()) {
                val id = parameter.identifier
                val range = TextRange.from(id.startOffsetInParent, id.textLength)
                holder.registerProblem(
                    parameter,
                    "Unused type parameter <code>#ref</code> #loc",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    range
                )
            }
        }
    }
}
