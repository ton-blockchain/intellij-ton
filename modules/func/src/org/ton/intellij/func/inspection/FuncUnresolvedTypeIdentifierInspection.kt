package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.ton.intellij.func.psi.FuncTypeIdentifier
import org.ton.intellij.func.psi.FuncVisitor
import org.ton.intellij.func.psi.impl.FuncTypeIdentifierImpl

class FuncUnresolvedTypeIdentifierInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitTypeIdentifier(o: FuncTypeIdentifier) {
            super.visitTypeIdentifier(o)
            if (o !is FuncTypeIdentifierImpl) return
            val reference = o.reference ?: return
            if (reference.resolve() == null) {
                val id = o.identifier
                val range = TextRange.from(id.startOffsetInParent, id.textLength)
                holder.registerProblem(
                    o,
                    "Unresolved type identifier <code>#ref</code>",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                    range
                )
            }
        }
    }
}
