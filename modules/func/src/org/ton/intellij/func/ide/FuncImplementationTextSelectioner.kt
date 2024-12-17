package org.ton.intellij.func.ide

import com.intellij.codeInsight.hint.ImplementationTextSelectioner
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.FuncFunction

class FuncImplementationTextSelectioner : ImplementationTextSelectioner {
    override fun getTextStartOffset(element: PsiElement): Int = textRange(element).startOffset

    override fun getTextEndOffset(element: PsiElement): Int = textRange(element).endOffset

    private fun textRange(element: PsiElement): TextRange = when (
        element
    ) {
        is FuncFunction -> element.textRange
        else -> element.textRange
    }
}
