package org.ton.intellij.asm

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.ton.intellij.asm.ide.AsmColor
import org.ton.intellij.asm.lexer.AsmLexer
import org.ton.intellij.asm.psi.AsmElementTypes

object AsmHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer {
        return AsmLexer()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            com.intellij.psi.TokenType.BAD_CHARACTER -> AsmColor.BAD_CHARACTER
            AsmElementTypes.INTEGER                  -> AsmColor.NUMBER
            AsmElementTypes.STACK_REGISTER           -> AsmColor.STACK_REGISTER
            AsmElementTypes.CONTROL_REGISTER         -> AsmColor.CONTROL_REGISTER
            else                                     -> AsmColor.INSTRUCTION
        }.let {
            pack(it.textAttributesKey)
        }
    }
}
