package org.ton.intellij.tolk.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tolk.ide.colors.TolkColor
import org.ton.intellij.tolk.lexer.TolkLexer
import org.ton.intellij.tolk.parser.TolkParserDefinition
import org.ton.intellij.tolk.psi.TOLK_DOC_COMMENTS
import org.ton.intellij.tolk.psi.TOLK_KEYWORDS
import org.ton.intellij.tolk.psi.TolkElementTypes.*

class TolkSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = TolkLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            RAW_STRING_ELEMENT,
            CLOSING_QUOTE,
            OPEN_QUOTE -> TolkColor.STRING
            INTEGER_LITERAL -> TolkColor.NUMBER
            SEMICOLON -> TolkColor.SEMICOLON
            COMMA -> TolkColor.COMMA
            DOT -> TolkColor.DOT
            AT -> TolkColor.ANNOTATION
            TolkParserDefinition.EOL_COMMENT -> TolkColor.LINE_COMMENT
            TolkParserDefinition.BLOCK_COMMENT -> TolkColor.BLOCK_COMMENT
            LBRACE, RBRACE -> TolkColor.BRACES
            LBRACK, RBRACK -> TolkColor.BRACKETS
            LPAREN, RPAREN -> TolkColor.PARENTHESES
            TRUE_KEYWORD, FALSE_KEYWORD -> TolkColor.KEYWORD
            in TOLK_KEYWORDS -> TolkColor.KEYWORD
            in TolkParserDefinition.STRING_LITERALS -> TolkColor.STRING
            in TolkParserDefinition.OPERATORS -> TolkColor.OPERATION_SIGN
            in TOLK_DOC_COMMENTS -> TolkColor.DOC_COMMENT
            else -> null
        }.let {
            pack(it?.textAttributesKey)
        }
}
