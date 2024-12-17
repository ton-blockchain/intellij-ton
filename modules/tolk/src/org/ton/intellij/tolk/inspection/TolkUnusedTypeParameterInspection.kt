package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.findParentOfType
import org.ton.intellij.tolk.psi.TolkTypeParameterList
import org.ton.intellij.tolk.psi.TolkTypeParameterListOwner
import org.ton.intellij.tolk.psi.TolkVisitor

class TolkUnusedTypeParameterInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitTypeParameterList(o: TolkTypeParameterList) {
            val parameterList = o.typeParameterList
            if (parameterList.isEmpty()) return
            val owner = o.findParentOfType<TolkTypeParameterListOwner>() ?: return
            val searchScope = LocalSearchScope(owner)
            parameterList.forEach { typeParameter ->
                val result = ReferencesSearch.search(typeParameter, searchScope).toList()
                if (result.none()) {
                    holder.registerProblem(
                        typeParameter,
                        "Unused type parameter <code>#ref</code> #loc",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    )
                }
            }
        }
    }
}
