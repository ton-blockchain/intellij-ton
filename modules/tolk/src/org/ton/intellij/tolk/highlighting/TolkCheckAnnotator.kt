package org.ton.intellij.tolk.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.highlighting.checkers.TolkCommonProblemsChecker

class TolkCheckAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!element.isValid) return

        val visitors = getCheckerVisitors(holder)

        for (visitor in visitors) {
            element.accept(visitor)
        }
    }

    private fun getCheckerVisitors(holder: AnnotationHolder): List<TolkVisitor> {
        return listOf(
            TolkCommonProblemsChecker(holder),
        )
    }
}
