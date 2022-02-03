package com.github.andreypfau.intellijton.fift.ide

import com.github.andreypfau.intellijton.fift.psi.FiftTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class FiftBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = BRACE_PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    companion object {
        val BRACE_PAIRS = arrayOf(
            BracePair(FiftTypes.LBRACE, FiftTypes.RBRACE, false),
            BracePair(FiftTypes.LPAREN, FiftTypes.RPAREN, false),
            BracePair(FiftTypes.LBRACKET, FiftTypes.RBRACKET, false),
        )
    }
}