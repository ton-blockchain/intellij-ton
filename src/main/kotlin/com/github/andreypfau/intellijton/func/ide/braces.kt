package com.github.andreypfau.intellijton.func.ide

import com.github.andreypfau.intellijton.func.psi.FuncTokenTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

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