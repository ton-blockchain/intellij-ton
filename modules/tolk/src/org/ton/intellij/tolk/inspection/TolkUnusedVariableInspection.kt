package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import org.ton.intellij.tolk.ide.fixes.RenameUnderscoreFix
import org.ton.intellij.tolk.psi.TolkVar
import org.ton.intellij.tolk.psi.TolkVisitor
import java.util.*

class TolkUnusedVariableInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitVar(element: TolkVar) {
            val id = element.identifier
            if (id.text == "_") return
            val reference = ReferencesSearch.search(element, element.useScope).findFirst()
            if (reference != null) return
            val fixes = LinkedList<LocalQuickFix>()
            fixes.add(RenameUnderscoreFix(element))

//            val parent = element.parent
//            if (parent is TolkVarStatement) {
//                fixes.add(RemoveElementFix(parent))
//            }

            val range = TextRange.from(id.startOffsetInParent, id.textLength)
            holder.registerProblem(
                element,
                "Unused variable <code>#ref</code> #loc",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                range,
                *fixes.toTypedArray()
            )
        }
    }
}
