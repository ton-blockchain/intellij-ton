package org.ton.intellij.tasm.ide

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tasm.lexer.TasmLexerAdapter
import org.ton.intellij.tasm.parser.TASM_BRACES
import org.ton.intellij.tasm.parser.TASM_BRACKETS
import org.ton.intellij.tasm.parser.TASM_KEYWORDS
import org.ton.intellij.tasm.psi.TasmTypes.*

object TasmSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = TasmLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            IDENTIFIER       -> TasmColor.INSTRUCTION
            INTEGER          -> TasmColor.NUMBER
            HEX              -> TasmColor.HEX_LITERAL
            BIN              -> TasmColor.BIN_LITERAL
            BOC              -> TasmColor.BOC_LITERAL
            STACK            -> TasmColor.STACK_REGISTER
            CTRL             -> TasmColor.CONTROL_REGISTER
            COMMENT          -> TasmColor.COMMENT
            ARROW            -> TasmColor.ARROW
            in TASM_KEYWORDS -> TasmColor.KEYWORD
            in TASM_BRACES   -> TasmColor.BRACES
            in TASM_BRACKETS -> TasmColor.BRACKETS
            else             -> null
        }.let {
            pack(it?.textAttributesKey)
        }
    }
}
