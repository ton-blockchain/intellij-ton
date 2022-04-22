package org.ton.intellij.fift.ide

import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import org.ton.intellij.fift.parser.FiftLexerAdapter
import org.ton.intellij.fift.psi.FiftTypes.*

class FiftSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = FiftSyntaxHighlighter
}

object FiftSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = FiftLexerAdapter()
    override fun getTokenHighlights(tokenType: IElementType) = when (tokenType) {
        in FiftParserDefinition.DOCUMENTATION -> FiftColor.DOCUMENTATION
        in FiftParserDefinition.COMMENTS -> FiftColor.COMMENT
        in FiftParserDefinition.BRACES -> FiftColor.BRACES
        in FiftParserDefinition.PARENTHESES -> FiftColor.PARENTHESES
        in FiftParserDefinition.BRACKETS -> FiftColor.BRACKETS

        NUMBER_DIGIT_LITERAL, NUMBER_HEX_LITERAL, NUMBER_BINARY_LITERAL -> FiftColor.NUMBER
        SLICE_BINARY_LITERAL, SLICE_HEX_LITERAL, BYTE_HEX_LITERAL -> FiftColor.NUMBER
        STRING_LITERAL -> FiftColor.STRING

        ABORT, PRINT, STRING_CONCAT -> FiftColor.STRING_WORD

        INCLUDE -> FiftColor.KEYWORD

        IF, IFNOT, COND -> FiftColor.KEYWORD
        TRUE, FALSE -> FiftColor.KEYWORD
        DUP, DROP, SWAP, ROT, REV_ROT, OVER, TUCK, NIP, DUP_DUP,
        DROP_DROP, SWAP_SWAP, PICK, ROLL, REV_ROLL, EXCH, EXCH2, COND_DUP -> FiftColor.KEYWORD

        else -> null
    }.let {
        pack(it?.textAttributesKey)
    }
}