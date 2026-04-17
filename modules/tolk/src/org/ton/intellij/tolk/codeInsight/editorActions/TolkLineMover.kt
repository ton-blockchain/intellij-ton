package org.ton.intellij.tolk.codeInsight.editorActions

import com.intellij.codeInsight.editorActions.moveUpDown.LineMover
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange
import com.intellij.codeInsight.editorActions.moveUpDown.StatementUpDownMover
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFile

abstract class TolkLineMover : LineMover() {
    override fun checkAvailable(editor: Editor, file: PsiFile, info: MoveInfo, down: Boolean): Boolean {
        if (file !is TolkFile) return false
        if (!super.checkAvailable(editor, file, info, down)) return false

        val originalRange = info.toMove ?: return false
        val psiRange = StatementUpDownMover.getElementRange(editor, file, originalRange) ?: return false
        val firstElement = psiRange.first ?: return false
        val lastElement = psiRange.second ?: return false

        val firstItem = findMovableAncestor(firstElement, RangeEndpoint.START) ?: return false
        val lastItem = findMovableAncestor(lastElement, RangeEndpoint.END) ?: return false

        if (!canApply(firstItem, lastItem)) {
            info.toMove2 = null
            return true
        }

        var sibling = StatementUpDownMover.firstNonWhiteElement(
            if (down) lastItem.nextSibling else firstItem.prevSibling,
            down,
        )
        if (sibling != null) sibling = fixupSibling(sibling, down)
        if (sibling == null) {
            info.toMove2 = null
            return true
        }

        val sourceRange = LineRange(firstItem, lastItem)
        info.toMove = sourceRange
        info.toMove.firstElement = firstItem
        info.toMove.lastElement = lastItem

        val whitespace = findTargetWhitespace(sibling, down)
        if (whitespace != null) {
            val nearLine = if (down) sourceRange.endLine else sourceRange.startLine - 1
            info.toMove2 = LineRange(nearLine, nearLine + 1)
            info.toMove2.firstElement = whitespace
        } else {
            val target = findTargetElement(sibling, down)
            if (target != null) {
                info.toMove2 = LineRange(target)
                info.toMove2.firstElement = target
            } else {
                info.toMove2 = null
            }
        }

        return true
    }

    protected abstract fun findMovableAncestor(psi: PsiElement, endpoint: RangeEndpoint): PsiElement?
    protected abstract fun findTargetElement(sibling: PsiElement, down: Boolean): PsiElement?
    protected open fun fixupSibling(sibling: PsiElement, down: Boolean): PsiElement? = sibling
    protected open fun canApply(firstMovableElement: PsiElement, secondMovableElement: PsiElement): Boolean = true
    protected open fun findTargetWhitespace(sibling: PsiElement, down: Boolean): PsiWhiteSpace? = null

    companion object {
        enum class RangeEndpoint {
            START,
            END,
        }

        fun isMovingOutOfBraceBlock(sibling: PsiElement, down: Boolean): Boolean =
            sibling.node.elementType == (if (down) TolkElementTypes.RBRACE else TolkElementTypes.LBRACE)

        fun PsiWhiteSpace.isMultiLine(): Boolean = text.count { it == '\n' } > 1
    }
}
