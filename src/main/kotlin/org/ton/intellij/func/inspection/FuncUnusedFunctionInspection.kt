package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncVisitor
import org.ton.intellij.func.psi.impl.hasMethodId

class FuncUnusedFunctionInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitFunction(o: FuncFunction) {
            val name = o.name ?: return
            if (o.hasMethodId) return
            if (name == "main") return
            if (name == "recv_internal") return
            if (name == "recv_external") return
            if (name == "run_ticktock") return
            if (o.containingFile.name == "stdlib.fc") return
            if (ReferencesSearch.search(o, o.useScope).findFirst() == null) {
                val id = o.identifier
                val range = TextRange.from(id.startOffsetInParent, id.textLength)
                holder.registerProblem(
                    o,
                    "Unused function <code>#ref</code> #loc",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    range
                )
            }
        }
    }
}
