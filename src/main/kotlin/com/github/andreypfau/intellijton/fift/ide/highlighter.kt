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
    private val tokenMapping = mapOf(
        COMMENT to FiftColor.COMMENT,
        LBRACE to FiftColor.BRACES,
        RBRACE to FiftColor.BRACES,
        LBRACKET to FiftColor.BRACKETS,
        RBRACKET to FiftColor.BRACKETS,
        LPAREN to FiftColor.PARENTHESES,
        RPAREN to FiftColor.PARENTHESES,

        STACK_WORD to FiftColor.KEYWORD,
        PRINT_WORD to FiftColor.ACTIVE_WORD,
        BOOLEAN to FiftColor.KEYWORD,

        NUMBER_LITERAL to FiftColor.NUMBER,
        NUMBER_BINARY_LITERAL to FiftColor.NUMBER,
        NUMBER_HEX_LITERAL to FiftColor.NUMBER,
        STRING_LITERAL to FiftColor.STRING,
    ).mapValues { it.value.textAttributesKey }

    override fun getHighlightingLexer() = FiftLexerAdapter()
    override fun getTokenHighlights(tokenType: IElementType) = pack(tokenMapping[tokenType])
}