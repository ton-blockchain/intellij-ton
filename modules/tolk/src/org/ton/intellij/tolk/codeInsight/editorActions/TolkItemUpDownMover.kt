package org.ton.intellij.tolk.codeInsight.editorActions

import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.codeInsight.editorActions.TolkLineMover.Companion.RangeEndpoint
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.psi.TolkEnum
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkGlobalVar
import org.ton.intellij.tolk.psi.TolkStatement
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkTypeDef

class TolkItemUpDownMover : TolkLineMover() {
    override fun findMovableAncestor(psi: PsiElement, endpoint: RangeEndpoint): PsiElement? {
        var current: PsiElement? = psi
        while (current != null && current !is TolkFile) {
            if (current is TolkStatement) {
                return null
            }
            if (isMovableItem(current)) {
                return current
            }
            current = current.parent
        }
        return null
    }

    override fun findTargetElement(sibling: PsiElement, down: Boolean): PsiElement? = sibling

    override fun canApply(firstMovableElement: PsiElement, secondMovableElement: PsiElement): Boolean =
        firstMovableElement.parent == secondMovableElement.parent && firstMovableElement.parent is TolkFile

    private fun isMovableItem(element: PsiElement): Boolean {
        if (element.parent !is TolkFile) return false
        return element is TolkFunction ||
            element is TolkConstVar ||
            element is TolkGlobalVar ||
            element is TolkStruct ||
            element is TolkEnum ||
            element is TolkTypeDef
    }
}
