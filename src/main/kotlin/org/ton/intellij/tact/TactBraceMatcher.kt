package org.ton.intellij.tact

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tact.psi.TactElementTypes

class TactBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = arrayOf(
        BracePair(TactElementTypes.LBRACE, TactElementTypes.RBRACE, true),
        BracePair(TactElementTypes.LPAREN, TactElementTypes.RPAREN, false),
        BracePair(TactElementTypes.LBRACK, TactElementTypes.RBRACK, false),
    )

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean {
        return true
    }

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int {
        return openingBraceOffset
    }
}
