package org.ton.intellij.func.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.ton.intellij.func.lexer.FuncLexer
import org.ton.intellij.func.parser.FuncParserDefinition
import org.ton.intellij.func.psi.FuncElementTypes

class FuncSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = FuncLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            FuncElementTypes.RAW_STRING_ELEMENT,
            FuncElementTypes.CLOSING_QUOTE,
            FuncElementTypes.OPEN_QUOTE -> FuncSyntaxHighlightingColors.STRING
            FuncElementTypes.INTEGER_LITERAL -> FuncSyntaxHighlightingColors.NUMBER
            FuncElementTypes.SEMICOLON -> FuncSyntaxHighlightingColors.SEMICOLON
            FuncElementTypes.COMMA -> FuncSyntaxHighlightingColors.COMMA
            FuncElementTypes.DOT -> FuncSyntaxHighlightingColors.DOT
            FuncElementTypes.LINE_COMMENT -> FuncSyntaxHighlightingColors.LINE_COMMENT
            FuncElementTypes.BLOCK_COMMENT -> FuncSyntaxHighlightingColors.BLOCK_COMMENT
            FuncElementTypes.DOC_COMMENT -> FuncSyntaxHighlightingColors.DOC_COMMENT
            FuncElementTypes.LBRACE, FuncElementTypes.RBRACE -> FuncSyntaxHighlightingColors.BRACES
            FuncElementTypes.LBRACK, FuncElementTypes.RBRACK -> FuncSyntaxHighlightingColors.BRACKETS
            FuncElementTypes.LPAREN, FuncElementTypes.RPAREN -> FuncSyntaxHighlightingColors.PARENTHESES
            in FuncParserDefinition.PRIMITIVE_TYPES -> FuncSyntaxHighlightingColors.PRIMITIVE_TYPES
            in FuncParserDefinition.KEYWORDS -> FuncSyntaxHighlightingColors.KEYWORD
            in FuncParserDefinition.STRING_LITERALS -> FuncSyntaxHighlightingColors.STRING
            in FuncParserDefinition.MACRO -> FuncSyntaxHighlightingColors.MACRO
            else -> null
        }.let {
//            println(" = $it")
            pack(it?.textAttributesKey)
        }
}
