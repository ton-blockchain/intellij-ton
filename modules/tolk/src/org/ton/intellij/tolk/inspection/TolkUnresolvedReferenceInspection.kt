package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.ton.intellij.tolk.ide.imports.TolkImportFileQuickFix
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkReferenceTypeExpression
import org.ton.intellij.tolk.psi.TolkVisitor

class TolkUnresolvedReferenceInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitReferenceTypeExpression(expr: TolkReferenceTypeExpression) {
            val reference = expr.reference ?: return
            checkReference(reference, expr, expr.identifier)
        }

        override fun visitReferenceExpression(expr: TolkReferenceExpression) {
            when (expr.referenceName) {
                "_" -> return
            }
            val reference = expr.reference ?: return
            checkReference(reference, expr, expr.identifier)
        }

        private fun checkReference(reference: PsiReference, expr: TolkElement, identifier: PsiElement) {
            val resolved = reference.resolve()
            if (resolved != null) return
            if (InjectedLanguageManager.getInstance(expr.project).isInjectedFragment(expr.containingFile)) return

            val range = TextRange.from(identifier.startOffsetInParent, identifier.textLength)
            val fixes = createImportFileFixes(expr, reference, holder.isOnTheFly)
            registerProblem(expr, range, fixes)
        }

        fun registerProblem(o: PsiElement, range: TextRange, fixes: Array<LocalQuickFix>) {
            // actual error reported in TolkAnnotator
            holder.registerProblem(o, "", ProblemHighlightType.INFORMATION, range, *fixes)
        }
    }

    private fun createImportFileFixes(target: TolkElement, reference: PsiReference, onTheFly: Boolean): Array<LocalQuickFix> {
        if (onTheFly) {
            val importFix = TolkImportFileQuickFix(reference)
            if (importFix.isAvailable(target.project, target.containingFile, target, target)) {
                return arrayOf(importFix)
            }
        }

        val filesToImport = TolkImportFileQuickFix.findImportVariants(reference.canonicalText, target)
        if (filesToImport.isNotEmpty()) {
            val result = mutableListOf<LocalQuickFix>()
            for (importPath in filesToImport) {
                val importFix = TolkImportFileQuickFix(target, importPath)
                if (importFix.isAvailable(target.project, target.containingFile, target, target)) {
                    result.add(importFix)
                }
            }
            return result.toTypedArray()
        }

        return LocalQuickFix.EMPTY_ARRAY
    }
}
