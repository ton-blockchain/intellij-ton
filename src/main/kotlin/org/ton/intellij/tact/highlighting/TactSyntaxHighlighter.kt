package org.ton.intellij.tact.highlighting

import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tact.parser._TactLexer
import org.ton.intellij.tact.psi.TACT_KEYWORDS
import org.ton.intellij.tact.psi.TACT_MACROS
import org.ton.intellij.tact.psi.TACT_STRING_LITERALS
import org.ton.intellij.tact.psi.TactElementTypes

class TactSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = FlexAdapter(_TactLexer())

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = when (tokenType) {
        TactElementTypes.INTEGER_LITERAL -> TactColor.NUMBER
        TactElementTypes.LINE_COMMENT -> TactColor.LINE_COMMENT
        TactElementTypes.BLOCK_COMMENT -> TactColor.BLOCK_COMMENT
        TactElementTypes.SEMICOLON -> TactColor.SEMICOLON
        TactElementTypes.COMMA -> TactColor.COMMA
        TactElementTypes.DOT -> TactColor.DOT
        TactElementTypes.LBRACE, TactElementTypes.RBRACE -> TactColor.BRACES
        TactElementTypes.LBRACK, TactElementTypes.RBRACK -> TactColor.BRACKETS
        TactElementTypes.LPAREN, TactElementTypes.RPAREN -> TactColor.PARENTHESES
        TactElementTypes.BOOLEAN_LITERAL, TactElementTypes.NULL_LITERAL -> TactColor.KEYWORD
        in TACT_STRING_LITERALS -> TactColor.STRING
        in TACT_MACROS -> TactColor.MACRO
        in TACT_KEYWORDS -> TactColor.KEYWORD
        else -> null
    }.let {
        pack(it?.textAttributesKey)
    }
}
