package org.ton.intellij.func.ide

import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import org.ton.intellij.func.lexer.FuncLexerAdapter
import org.ton.intellij.func.psi.FuncTokenTypes.*

class FuncSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = FuncSyntaxHighlighter
}

object FuncSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = FuncLexerAdapter()
    override fun getTokenHighlights(tokenType: IElementType) = when (tokenType) {
        in FuncParserDefinition.DOCUMENTATION -> FuncColor.DOCUMENTATION
        in FuncParserDefinition.COMMENTS -> FuncColor.COMMENT
        in FuncParserDefinition.KEYWORDS -> FuncColor.KEYWORD
        in FuncParserDefinition.TYPES -> FuncColor.PRIMITIVE_TYPES
        in FuncParserDefinition.BRACES -> FuncColor.BRACES
        in FuncParserDefinition.PARENTHESES -> FuncColor.PARENTHESES
        in FuncParserDefinition.BRACKETS -> FuncColor.BRACKETS
        in FuncParserDefinition.OPERATORS -> FuncColor.OPERATION_SIGN
        PRAGMA, INCLUDE, VERSION, NOT_VERSION, TEST_VERSION_SET -> FuncColor.MACRO
        DOT -> FuncColor.DOT
        COMMA -> FuncColor.COMMA
        SEMICOLON -> FuncColor.SEMICOLON
        INTEGER_LITERAL -> FuncColor.NUMBER
        STRING_LITERAL -> FuncColor.STRING
        else -> null
    }.let {
        pack(it?.textAttributesKey)
    }
}
