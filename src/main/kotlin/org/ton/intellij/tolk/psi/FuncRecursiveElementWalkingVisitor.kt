package org.ton.intellij.tolk.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveVisitor
import com.intellij.psi.PsiWalkingState

abstract class TolkRecursiveElementWalkingVisitor : TolkVisitor(), PsiRecursiveVisitor {
    private val state = object : PsiWalkingState(this) {
        override fun elementFinished(element: PsiElement) {
            this@TolkRecursiveElementWalkingVisitor.elementFinished(element)
        }
    }

    override fun visitElement(element: TolkElement) {
        state.elementStarted(element)
    }

    protected open fun elementFinished(element: PsiElement) {
    }

    override fun visitReferenceExpression(o: TolkReferenceExpression) {
        visitExpression(o)
        state.startedWalking()
        super.visitReferenceExpression(o)
    }

    fun stopWalking() {
        state.stopWalking()
    }
}
