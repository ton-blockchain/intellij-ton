package org.ton.intellij.func

import com.intellij.codeInsight.hint.DeclarationRangeUtil
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.ton.intellij.func.parser.FuncParserDefinition
import org.ton.intellij.func.psi.FuncBlockStatement
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncFunctionBody

class FuncBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(FuncElementTypes.LBRACE, FuncElementTypes.RBRACE, true),
        BracePair(FuncElementTypes.LPAREN, FuncElementTypes.RPAREN, false),
        BracePair(FuncElementTypes.LBRACK, FuncElementTypes.RBRACK, false),
    )

    override fun getPairs(): Array<BracePair> = pairs

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean {
        return FuncParserDefinition.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(contextType)
                || contextType == FuncElementTypes.COLON
                || contextType == FuncElementTypes.SEMICOLON
                || contextType == FuncElementTypes.COMMA
                || contextType == FuncElementTypes.RPAREN
                || contextType == FuncElementTypes.RBRACK
                || contextType == FuncElementTypes.RBRACE
                || contextType == FuncElementTypes.LBRACE
    }

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int {
        val element = file?.findElementAt(openingBraceOffset)
        if (element == null || element is PsiFile) return openingBraceOffset
        return when (val parent = element.parent) {
            is FuncFunctionBody, is FuncBlockStatement ->
                DeclarationRangeUtil.getPossibleDeclarationAtRange(parent.parent)?.startOffset ?: openingBraceOffset

            else -> openingBraceOffset
        }
    }
}
