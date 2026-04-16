package org.ton.intellij.tolk.codeInsight.editorActions

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.ton.intellij.tolk.codeInsight.editorActions.TolkLineMover.Companion.RangeEndpoint
import org.ton.intellij.tolk.codeInsight.editorActions.TolkLineMover.Companion.isMultiLine
import org.ton.intellij.tolk.psi.TolkBlockStatement
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkStatement

class TolkStatementUpDownMover : TolkLineMover() {
    override fun findMovableAncestor(psi: PsiElement, endpoint: RangeEndpoint): PsiElement? {
        var current: PsiElement? = psi
        while (current != null && current !is TolkFile) {
            if (isMovableStatement(current)) {
                return current
            }
            current = current.parent
        }
        return null
    }

    override fun findTargetElement(sibling: PsiElement, down: Boolean): PsiElement? {
        if (isMovingOutOfBraceBlock(sibling, down) && sibling.parent is TolkBlockStatement) {
            return null
        }
        return sibling
    }

    override fun canApply(firstMovableElement: PsiElement, secondMovableElement: PsiElement): Boolean =
        firstMovableElement.parent == secondMovableElement.parent && firstMovableElement.parent is TolkBlockStatement

    override fun findTargetWhitespace(sibling: PsiElement, down: Boolean): PsiWhiteSpace? {
        val whitespace = (if (down) sibling.prevSibling else sibling.nextSibling) as? PsiWhiteSpace ?: return null
        return whitespace.takeIf { it.isMultiLine() }
    }

    private fun isMovableStatement(element: PsiElement): Boolean {
        if (element.parent !is TolkBlockStatement) return false
        return element is TolkStatement || element is PsiComment
    }
}
