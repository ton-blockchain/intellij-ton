package com.github.andreypfau.intellijton.fift.ide

import com.github.andreypfau.intellijton.fift.parser.FiftLexerAdapter
import com.github.andreypfau.intellijton.fift.psi.FiftTypes.*
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class FiftSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = FiftSyntaxHighlighter
}

object FiftSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = FiftLexerAdapter()
    override fun getTokenHighlights(tokenType: IElementType) = when (tokenType) {
        COMMENT -> FiftColor.COMMENT
        LBRACE, RBRACE -> FiftColor.BRACES
        LBRACKET, RBRACKET -> FiftColor.BRACKETS
        LPAREN, RPAREN -> FiftColor.PARENTHESES

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