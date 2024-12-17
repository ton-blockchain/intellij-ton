package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkVisitor

class TolkUnusedVariableInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitReferenceExpression(o: TolkReferenceExpression) {
            return // TODO fix
//            super.visitReferenceExpression(o)
//            val id = o.identifier
//            if (id.text == "_") return
//            if (ReferencesSearch.search(o, o.useScope).findFirst() == null) {
//                val range = TextRange.from(id.startOffsetInParent, id.textLength)
//                val parent = o.parent
//                val fixes = mutableListOf<LocalQuickFix>()
//
//                if (parent is TolkTensorExpression || parent is TolkTupleExpression) {
//                    // TODO: fix underscore fix
////                    fixes.add(RenameUnderscoreFix(o))
//                }
//                if (parent is TolkApplyExpression) {
//                    val grandParent = parent.parent
//                    if (grandParent is TolkBinExpression && grandParent.left == parent) {
//                        val stmt = grandParent.ancestorStrict<TolkExpressionStatement>()
//                        if (stmt != null) {
//                            fixes.add(RemoveElementFix(stmt))
//                        }
//                    }
//                }
//
//                holder.registerProblem(
//                    o,
//                    "Unused variable <code>#ref</code> #loc",
//                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
//                    range,
//                    *fixes.toTypedArray()
//                )
//            }
        }
    }
}
