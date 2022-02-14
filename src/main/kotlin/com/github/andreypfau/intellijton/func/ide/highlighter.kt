package com.github.andreypfau.intellijton.func.ide

import com.github.andreypfau.intellijton.func.parser.FuncLexerAdapter
import com.github.andreypfau.intellijton.func.psi.FuncTypes.*
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class FuncSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = FuncSyntaxHighlighter
}

object FuncSyntaxHighlighter : SyntaxHighlighterBase() {
    private val tokenMapping = mapOf(
        COMMENT to FuncColor.COMMENT,
        LBRACE to FuncColor.BRACES,
        RBRACE to FuncColor.BRACES,
        LBRACKET to FuncColor.BRACKETS,
        RBRACKET to FuncColor.BRACKETS,
        LPAREN to FuncColor.PARENTHESES,
        RPAREN to FuncColor.PARENTHESES,
        SEMICOLON to FuncColor.SEMICOLON,
        COMMA to FuncColor.COMMA,
        DOT to FuncColor.DOT,

        STRING_LITERAL to FuncColor.STRING,
    )
        .plus(keywords().map { it to FuncColor.KEYWORD })
        .plus(primitiveTypes().map { it to FuncColor.KEYWORD })
        .plus(booleanLiterals().map { it to FuncColor.KEYWORD })
        .plus(functionKeywords().map { it to FuncColor.KEYWORD })
        .plus(numberLiterals().map { it to FuncColor.NUMBER })
        .plus(operatorSigns().map { it to FuncColor.OPERATION_SIGN })
        .mapValues { it.value.textAttributesKey }

    private fun keywords() = setOf(
        RETURN, REPEAT, IF, IFNOT, ELSEIF, ELSEIFNOT, ELSE, DO, UNTIL, WHILE, TYPE, VAR, GLOBAL
    )

    private fun functionKeywords() = setOf(
        FORALL, IMPURE, INLINE, INLINE_REF, METHOD_ID, ASM
    )

    private fun primitiveTypes() = setOf(
        INT, CELL, SLICE, BUILDER, CONT, TUPLE
    )

    private fun booleanLiterals() = setOf(
        TRUE, FALSE
    )

    private fun numberLiterals() = setOf(
        DECIMNAL_NUMBER_LITERAL, HEX_NUMBER_LITERAL, BINARY_NUMBER_LITERAL
    )

    private fun operatorSigns() = setOf(
        PLUS, MINUS, DIV, MULT, ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, DIV_ASSIGN, MULT_ASSIGN
    )

    override fun getHighlightingLexer() = FuncLexerAdapter()
    override fun getTokenHighlights(tokenType: IElementType) = pack(tokenMapping[tokenType])
}