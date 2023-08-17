package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.right

class FuncUnusedVariableInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitVarExpression(o: FuncVarExpression) {
            val right = o.right ?: return
            if (right is FuncReferenceExpression) {
                processVarExpression(right, holder)
            }
            if (right is FuncTensorExpression) {
                for (tensorElement in right.expressionList) {
                    ProgressIndicatorProvider.checkCanceled()
                    when (tensorElement) {
                        is FuncVarExpression -> visitVarExpression(tensorElement)
                        is FuncReferenceExpression -> processVarExpression(tensorElement, holder)
                    }
                }
            }
        }
    }

    private fun processVarExpression(
        element: FuncNamedElement,
        holder: ProblemsHolder,
    ) {
        val id = element.identifier ?: return
        if (ReferencesSearch.search(element, element.useScope).findFirst() == null) {
            val range = TextRange.from(id.startOffsetInParent, id.textLength)
            holder.registerProblem(
                element,
                "Unused variable <code>#ref</code> #loc",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                range
            )
        }
    }
}
