package com.github.andreypfau.intellijton.func.ide

import com.github.andreypfau.intellijton.func.lexer.FuncLexerAdapter
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
    override fun getHighlightingLexer() = FuncLexerAdapter()
    override fun getTokenHighlights(tokenType: IElementType) = when(tokenType) {
        in FuncParserDefinition.DOCUMENTATION -> FuncColor.DOCUMENTATION
        in FuncParserDefinition.COMMENTS -> FuncColor.COMMENT
        in FuncParserDefinition.KEYWORDS -> FuncColor.KEYWORD
        in FuncParserDefinition.TYPES -> FuncColor.PRIMITIVE_TYPES
        in FuncParserDefinition.BRACES -> FuncColor.BRACES
        in FuncParserDefinition.PARENTHESES -> FuncColor.PARENTHESES
        in FuncParserDefinition.BRACKETS ->  FuncColor.BRACKETS
        in FuncParserDefinition.OPERATORS -> FuncColor.OPERATION_SIGN
        DOT -> FuncColor.DOT
        COMMA -> FuncColor.COMMA
        SEMICOLON -> FuncColor.SEMICOLON
        INTEGER_LITERAL -> FuncColor.NUMBER
        STRING_LITERAL -> FuncColor.STRING
        else -> null
    }.let {
        println("$tokenType -> $it")
        pack(it?.textAttributesKey)
    }
}
