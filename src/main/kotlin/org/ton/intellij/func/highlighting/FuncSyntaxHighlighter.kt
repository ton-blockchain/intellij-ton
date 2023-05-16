package org.ton.intellij.func.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.ton.intellij.func.lexer.FuncLexer
import org.ton.intellij.func.parser.FuncParserDefinition

class FuncSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = FuncLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType.also { print("highlight: ${tokenType.debugName}") }) {
            in FuncParserDefinition.MACRO -> FuncSyntaxHighlightingColors.MACRO
            in FuncParserDefinition.STRING_LITERALS -> FuncSyntaxHighlightingColors.STRING
            else -> null
        }.let {
            println(" = $it")
            pack(it?.textAttributesKey)
        }
}
