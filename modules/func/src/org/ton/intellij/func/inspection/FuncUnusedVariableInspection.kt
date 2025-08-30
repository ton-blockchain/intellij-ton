package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import org.ton.intellij.func.ide.fixes.RemoveElementFix
import org.ton.intellij.func.ide.fixes.RenameUnderscoreFix
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.isVariableDefinition
import org.ton.intellij.util.ancestorStrict

class FuncUnusedVariableInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitReferenceExpression(o: FuncReferenceExpression) {
            super.visitReferenceExpression(o)
            if (!o.isVariableDefinition()) return
            val id = o.identifier
            if (id.text == "_") return
            if (ReferencesSearch.search(o, o.useScope).findFirst() == null) {
                val range = TextRange.from(id.startOffsetInParent, id.textLength)
                val parent = o.parent
                val fixes = mutableListOf<LocalQuickFix>()

                if (parent is FuncTensorExpression || parent is FuncTupleExpression) {
                    fixes.add(RenameUnderscoreFix(o))
                }
                if (parent is FuncApplyExpression) {
                    val grandParent = parent.parent
                    if (grandParent is FuncBinExpression && grandParent.left == parent) {
                        val stmt = grandParent.ancestorStrict<FuncExpressionStatement>()
                        if (stmt != null) {
                            fixes.add(RemoveElementFix(stmt))
                        }
                    }

                    // (int a, int b) = ...
                    // ^^^^^^^^^^^^^^ this
                    if (grandParent is FuncTensorExpression) {
                        // (int a, int b) = ...
                        // ^^^^^^^^^^^^^^^^^^^^ this
                        val grandGrandParent = grandParent.parent
                        if (grandGrandParent is FuncBinExpression && grandGrandParent.left == grandParent) {
                            // rename `int a` -> (_, int b)
                            fixes.add(RenameUnderscoreFix(o.name ?: "", parent))
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
