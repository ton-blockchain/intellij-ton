package org.ton.intellij.tact.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveVisitor
import com.intellij.psi.PsiWalkingState

abstract class TactRecursiveElementWalkingVisitor : TactVisitor(), PsiRecursiveVisitor {
    private val walkingState = object : PsiWalkingState(this) {
        override fun elementFinished(element: PsiElement) {
            this@TactRecursiveElementWalkingVisitor.elementFinished(element)
        }
    }

    override fun visitElement(element: PsiElement) {
        walkingState.elementStarted(element)
    }

    override fun visitElement(o: TactElement) {
        walkingState.elementStarted(o)
    }

    protected fun elementFinished(element: PsiElement) {
    }

    fun stopWalking() {
        walkingState.stopWalking()
    }
}
