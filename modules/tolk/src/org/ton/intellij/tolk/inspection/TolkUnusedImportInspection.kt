package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.codeInsight.imports.TolkImportOptimizer
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkIncludeDefinition
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.psi.impl.path

class TolkUnusedImportInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): TolkVisitor {
        val file = holder.file as? TolkFile ?: return object : TolkVisitor() {}
        val unusedImports = TolkImportOptimizer.collectUnusedImports(file)

        return object : TolkVisitor() {
            override fun visitIncludeDefinition(import: TolkIncludeDefinition) {
                if (unusedImports.contains(import)) {
                    holder.registerProblem(
                        import,
                        TolkBundle.message("inspection.tolk.unused.import.message", import.path),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        OPTIMIZE_IMPORTS_QUICK_FIX
                    )
                }
            }
        }
    }
}

val OPTIMIZE_IMPORTS_QUICK_FIX = object : LocalQuickFix {
    override fun getFamilyName() = TolkBundle.message("inspection.tolk.optimize.imports.family.name")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val file = element.containingFile
        TolkImportOptimizer().processFile(file).run()
    }
}
