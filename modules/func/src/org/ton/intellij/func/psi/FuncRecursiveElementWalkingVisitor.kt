package org.ton.intellij.func.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveVisitor
import com.intellij.psi.PsiWalkingState

abstract class FuncRecursiveElementWalkingVisitor : FuncVisitor(), PsiRecursiveVisitor {
    private val state = object : PsiWalkingState(this) {
        override fun elementFinished(element: PsiElement) {
            this@FuncRecursiveElementWalkingVisitor.elementFinished(element)
        }
    }

    override fun visitElement(element: FuncElement) {
        state.elementStarted(element)
    }

    protected open fun elementFinished(element: PsiElement) {
    }

    override fun visitReferenceExpression(o: FuncReferenceExpression) {
        visitExpression(o)
        state.startedWalking()
        super.visitReferenceExpression(o)
    }

    fun stopWalking() {
        state.stopWalking()
    }
}
