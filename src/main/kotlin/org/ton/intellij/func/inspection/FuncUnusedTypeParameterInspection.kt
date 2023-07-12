package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncTypeParameter
import org.ton.intellij.func.psi.FuncVisitor

class FuncUnusedTypeParameterInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitFunction(o: FuncFunction) {
            val parameters = o.typeParameterList?.typeParameterList ?: return
            for (parameter in parameters) {
                ProgressIndicatorProvider.checkCanceled()
                processParameter(parameter)
            }
        }

        private fun processParameter(parameter: FuncTypeParameter) {
            if (ReferencesSearch.search(parameter, parameter.useScope).findFirst() == null) {
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
