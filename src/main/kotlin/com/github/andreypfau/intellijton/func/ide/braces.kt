package com.github.andreypfau.intellijton.func.ide

import com.github.andreypfau.intellijton.func.psi.FuncTypes
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
            BracePair(FuncTypes.LBRACE, FuncTypes.RBRACE, false),
            BracePair(FuncTypes.LPAREN, FuncTypes.RPAREN, false),
            BracePair(FuncTypes.LBRACKET, FuncTypes.RBRACKET, false),
        )
    }
}