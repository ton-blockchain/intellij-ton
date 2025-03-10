package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.psi.impl.isGetMethod

class TolkUnusedFunctionInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitFunction(o: TolkFunction) {
            val name = o.name ?: return
            if (o.isGetMethod) return
            if (name == "main") return
            if (name == "onInternalMessage") return
            if (name == "onExternalMessage") return
            if (name == "onRunTickTock") return
            if (name == "onSplitPrepare") return
            if (name == "onSplitInstall") return
            val containingDirectory = o.containingFile.containingDirectory ?: return
            if (containingDirectory.name.startsWith("tolk-stdlib")) return
            if (ReferencesSearch.search(o, o.useScope).findFirst() == null) {
                val id = o.identifier ?: return
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
