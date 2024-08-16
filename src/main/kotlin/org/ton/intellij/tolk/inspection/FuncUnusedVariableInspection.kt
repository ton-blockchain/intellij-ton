package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import org.ton.intellij.tolk.ide.fixes.RemoveElementFix
import org.ton.intellij.tolk.ide.fixes.RenameUnderscoreFix
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.isVariableDefinition
import org.ton.intellij.util.ancestorStrict

class TolkUnusedVariableInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitReferenceExpression(o: TolkReferenceExpression) {
            super.visitReferenceExpression(o)
            if (!o.isVariableDefinition()) return
            val id = o.identifier
            if (id.text == "_") return
            if (ReferencesSearch.search(o, o.useScope).findFirst() == null) {
                val range = TextRange.from(id.startOffsetInParent, id.textLength)
                val parent = o.parent
                val fixes = mutableListOf<LocalQuickFix>()

                if (parent is TolkTensorExpression || parent is TolkTupleExpression) {
                    fixes.add(RenameUnderscoreFix(o))
                }
                if (parent is TolkApplyExpression) {
                    val grandParent = parent.parent
                    if (grandParent is TolkBinExpression && grandParent.left == parent) {
                        val stmt = grandParent.ancestorStrict<TolkExpressionStatement>()
                        if (stmt != null) {
                            fixes.add(RemoveElementFix(stmt))
                        }
                    }
                }

                holder.registerProblem(
                    o,
                    "Unused variable <code>#ref</code> #loc",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    range,
                    *fixes.toTypedArray()
                )
            }
        }
    }
}
