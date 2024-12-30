package org.ton.intellij.tlb.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tlb.psi.TlbImplicitField
import org.ton.intellij.tlb.psi.TlbNegatedTypeExpression
import org.ton.intellij.tlb.psi.TlbParamList
import org.ton.intellij.tlb.psi.TlbVisitor

class TlbUninitializedImplicitFieldInspection : TlbInspectionBase() {
    override fun buildTlbVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
    ): TlbVisitor = object : TlbVisitor() {
        override fun visitImplicitField(o: TlbImplicitField) {
            val isNat = o.tag != null
            val allUsages = ReferencesSearch.search(o, o.useScope)
            val initializers = allUsages.filtering {
                val element = it.element
                (isNat && element.parent is TlbNegatedTypeExpression) || element.parentOfType<TlbParamList>() != null
            }
            if (initializers.none()) {
                allUsages.forEach {
                    holder.registerProblem(
                        it.element,
                        "Implicit field `${o.name}` is not initialized",
                    )
                }
            }
        }
    }
}