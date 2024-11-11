package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkIncludeDefinition
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkVisitor

class TolkUnresolvedReferenceInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitReferenceExpression(o: TolkReferenceExpression) {
            super.visitReferenceExpression(o)
            val reference = o.reference ?: return
            val resolved = reference.resolve()
            if (resolved != null) return
            val id = o.identifier
            val range = TextRange.from(id.startOffsetInParent, id.textLength)
            if(InjectedLanguageManager.getInstance(o.project).isInjectedFragment(o.containingFile)) return
            registerProblem(o, range)
        }

        override fun visitIncludeDefinition(o: TolkIncludeDefinition) {
            super.visitIncludeDefinition(o)
            val reference = o.reference ?: return
            val resolved = reference.resolve()
            if (resolved != null) return
            val str = o.stringLiteral ?: return
            val rawStr = str.rawString ?: return
            val range = TextRange.from(rawStr.startOffsetInParent, rawStr.textLength)
            if(InjectedLanguageManager.getInstance(o.project).isInjectedFragment(o.containingFile)) return
            registerProblem(str, range)
        }

        fun registerProblem(o: PsiElement, range: TextRange) {
            holder.registerProblem(
                o,
                "Unresolved reference <code>#ref</code>",
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                range
            )
        }
    }
}
