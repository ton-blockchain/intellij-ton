package com.github.andreypfau.intellijton.tlb.ide

import com.github.andreypfau.intellijton.tlb.psi.TlbTypes.*
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class TlbBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = BRACE_PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    companion object {
        val BRACE_PAIRS = arrayOf(
            BracePair(LBRACE, RBRACE, false),
            BracePair(LPAREN, RPAREN, false),
            BracePair(LBRACKET, RBRACKET, false),
        )
    }
}