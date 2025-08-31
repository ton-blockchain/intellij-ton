package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.ton.intellij.func.codeInsight.imports.FuncImportOptimizer
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.FuncIncludeDefinition
import org.ton.intellij.func.psi.FuncVisitor
import org.ton.intellij.func.psi.impl.path

class FuncUnusedIncludeInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitIncludeDefinition(o: FuncIncludeDefinition) {
            val file = o.containingFile as? FuncFile ?: return
            val unusedIncludes = FuncImportOptimizer.collectUnusedIncludes(file)
            
            if (unusedIncludes.contains(o)) {
                holder.registerProblem(
                    o,
                    "Unused include '${o.path}'",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                )
            }
        }
    }
}
