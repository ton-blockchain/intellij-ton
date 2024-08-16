package org.ton.intellij.tolk.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tolk.lexer.TolkLexer
import org.ton.intellij.tolk.parser.TolkParserDefinition
import org.ton.intellij.tolk.psi.FUNC_DOC_COMMENTS
import org.ton.intellij.tolk.psi.FUNC_KEYWORDS
import org.ton.intellij.tolk.psi.TolkElementTypes

class TolkSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = TolkLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            TolkElementTypes.RAW_STRING_ELEMENT,
            TolkElementTypes.CLOSING_QUOTE,
            TolkElementTypes.OPEN_QUOTE,
            -> TolkColor.STRING

            TolkElementTypes.INTEGER_LITERAL -> TolkColor.NUMBER
            TolkElementTypes.SEMICOLON -> TolkColor.SEMICOLON
            TolkElementTypes.COMMA -> TolkColor.COMMA
//            TolkElementTypes.DOT -> TolkColor.DOT
            TolkParserDefinition.EOL_COMMENT -> TolkColor.LINE_COMMENT
            TolkParserDefinition.BLOCK_COMMENT -> TolkColor.BLOCK_COMMENT
            in FUNC_DOC_COMMENTS -> TolkColor.DOC_COMMENT
            TolkElementTypes.LBRACE, TolkElementTypes.RBRACE -> TolkColor.BRACES
            TolkElementTypes.LBRACK, TolkElementTypes.RBRACK -> TolkColor.BRACKETS
            TolkElementTypes.LPAREN, TolkElementTypes.RPAREN -> TolkColor.PARENTHESES
            in TolkParserDefinition.PRIMITIVE_TYPES -> TolkColor.PRIMITIVE_TYPES
            in FUNC_KEYWORDS -> TolkColor.KEYWORD
            in TolkParserDefinition.STRING_LITERALS -> TolkColor.STRING
            TolkElementTypes.SHA -> TolkColor.MACRO
            in TolkParserDefinition.OPERATORS -> TolkColor.OPERATION_SIGN
            else -> null
        }.let {
            pack(it?.textAttributesKey)
        }
}
