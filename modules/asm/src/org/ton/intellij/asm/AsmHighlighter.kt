package org.ton.intellij.asm

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.XmlHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.ton.intellij.asm.lexer.AsmLexer
import org.ton.intellij.asm.psi.AsmElementTypes

object AsmHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer {
        return AsmLexer()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            com.intellij.psi.TokenType.BAD_CHARACTER -> BAD_CHARACTER
            AsmElementTypes.INTEGER -> NUMBER
            AsmElementTypes.STACK_REGISTER -> STACK_REGISTER
            AsmElementTypes.CONTROL_REGISTER -> CONTROL_REGISTER
            else -> INSTRUCTION
        }.let {
            pack(it)
        }
    }

    private val BAD_CHARACTER = createTextAttributesKey("ASM.BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
    private val INSTRUCTION =
        createTextAttributesKey("ASM.INSTRUCTION", XmlHighlighterColors.HTML_TAG)
    private val NUMBER = createTextAttributesKey("ASM.NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    private val STACK_REGISTER =
        createTextAttributesKey("ASM.STACK_REGISTER", DefaultLanguageHighlighterColors.CONSTANT)
    private val CONTROL_REGISTER =
        createTextAttributesKey("ASM.STACK_REGISTER", DefaultLanguageHighlighterColors.CONSTANT)
}
