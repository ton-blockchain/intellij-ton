package org.ton.intellij.tolk.codeInsight.hint.codeVision

import com.intellij.codeInsight.hints.VcsCodeVisionCurlyBracketLanguageContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.tolk.psi.*
import java.awt.event.MouseEvent

class TolkVcsCodeVisionContext : VcsCodeVisionCurlyBracketLanguageContext() {
    override fun isRBrace(element: PsiElement): Boolean = element.elementType == TolkElementTypes.RBRACE

    override fun isAccepted(element: PsiElement): Boolean {
        return when (element) {
            is TolkFunction,
            is TolkStruct,
            is TolkTypeDef,
            is TolkGlobalVar,
            is TolkConstVar -> true
            else -> false
        }
    }

    override fun handleClick(
        mouseEvent: MouseEvent,
        editor: Editor,
        element: PsiElement
    ) {
    }
}
