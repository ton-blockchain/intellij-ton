package org.ton.intellij.func.ide

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.ton.intellij.func.psi.FuncTokenTypes

class FuncBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = BRACE_PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    companion object {
        val BRACE_PAIRS = arrayOf(
            BracePair(FuncTokenTypes.LBRACE, FuncTokenTypes.RBRACE, false),
            BracePair(FuncTokenTypes.LPAREN, FuncTokenTypes.RPAREN, false),
            BracePair(FuncTokenTypes.LBRACKET, FuncTokenTypes.RBRACKET, false),
        )
    }
}