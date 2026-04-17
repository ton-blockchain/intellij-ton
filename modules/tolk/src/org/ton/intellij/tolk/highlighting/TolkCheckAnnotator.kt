package org.ton.intellij.tolk.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.highlighting.checkers.TolkCommonProblemsChecker
import org.ton.intellij.tolk.psi.TolkVisitor

class TolkCheckAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!element.isValid) return

        val visitors = getCheckerVisitors(holder)

        for (visitor in visitors) {
            element.accept(visitor)
        }
    }

    private fun getCheckerVisitors(holder: AnnotationHolder): List<TolkVisitor> = listOf(
        TolkCommonProblemsChecker(holder),
    )
}
