package org.ton.intellij.tolk.ide

import com.intellij.codeInsight.hint.DeclarationRangeUtil
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tolk.parser.TolkParserDefinition
import org.ton.intellij.tolk.psi.TolkBlockStatement
import org.ton.intellij.tolk.psi.TolkElementTypes

class TolkBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(TolkElementTypes.LBRACE, TolkElementTypes.RBRACE, true),
        BracePair(TolkElementTypes.LPAREN, TolkElementTypes.RPAREN, false),
        BracePair(TolkElementTypes.LBRACK, TolkElementTypes.RBRACK, false),
    )

    override fun getPairs(): Array<BracePair> = pairs

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean {
        return TolkParserDefinition.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(contextType)
                || contextType == TolkElementTypes.COLON
                || contextType == TolkElementTypes.SEMICOLON
                || contextType == TolkElementTypes.COMMA
                || contextType == TolkElementTypes.RPAREN
                || contextType == TolkElementTypes.RBRACK
                || contextType == TolkElementTypes.RBRACE
                || contextType == TolkElementTypes.LBRACE
    }

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int {
        val element = file?.findElementAt(openingBraceOffset)
        if (element == null || element is PsiFile) return openingBraceOffset
        return when (val parent = element.parent) {
            is TolkBlockStatement ->
                DeclarationRangeUtil.getPossibleDeclarationAtRange(parent.parent)?.startOffset ?: openingBraceOffset

            else -> openingBraceOffset
        }
    }
}
