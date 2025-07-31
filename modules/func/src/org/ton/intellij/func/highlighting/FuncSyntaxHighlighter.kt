package org.ton.intellij.func.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.ton.intellij.func.lexer.FuncLexer
import org.ton.intellij.func.parser.FuncParserDefinition
import org.ton.intellij.func.psi.FUNC_DOC_COMMENTS
import org.ton.intellij.func.psi.FUNC_KEYWORDS
import org.ton.intellij.func.psi.FuncElementTypes

class FuncSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = FuncLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            FuncElementTypes.RAW_STRING_ELEMENT,
            FuncElementTypes.CLOSING_QUOTE,
            FuncElementTypes.OPEN_QUOTE,
                -> FuncColor.STRING

            FuncElementTypes.INTEGER_LITERAL -> FuncColor.NUMBER
            FuncElementTypes.SEMICOLON -> FuncColor.SEMICOLON
            FuncElementTypes.COMMA -> FuncColor.COMMA
            FuncParserDefinition.EOL_COMMENT -> FuncColor.LINE_COMMENT
            FuncParserDefinition.BLOCK_COMMENT -> FuncColor.BLOCK_COMMENT
            in FUNC_DOC_COMMENTS -> FuncColor.DOC_COMMENT
            FuncElementTypes.LBRACE, FuncElementTypes.RBRACE -> FuncColor.BRACES
            FuncElementTypes.LBRACK, FuncElementTypes.RBRACK -> FuncColor.BRACKETS
            FuncElementTypes.LPAREN, FuncElementTypes.RPAREN -> FuncColor.PARENTHESES
            in FuncParserDefinition.PRIMITIVE_TYPES -> FuncColor.PRIMITIVE_TYPES
            in FUNC_KEYWORDS -> FuncColor.KEYWORD
            in FuncParserDefinition.STRING_LITERALS -> FuncColor.STRING
            FuncElementTypes.SHA -> FuncColor.MACRO
            in FuncParserDefinition.OPERATORS -> FuncColor.OPERATION_SIGN
            else -> null
        }.let {
            pack(it?.textAttributesKey)
        }
}
