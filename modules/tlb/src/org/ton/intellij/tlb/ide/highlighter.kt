package org.ton.intellij.tlb.ide

import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tlb.lexer.TlbLexerAdapter
import org.ton.intellij.tlb.psi.TlbTypes.*

class TlbSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = TlbSyntaxHighlighter
}

object TlbSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = TlbLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType) = when (tokenType) {
        in TlbParserDefinition.DOCUMENTATION -> TlbColor.DOCUMENTATION
        in TlbParserDefinition.COMMENTS -> TlbColor.COMMENT
        in TlbParserDefinition.BRACES -> TlbColor.BRACES
        in TlbParserDefinition.BRACKETS -> TlbColor.BRACKETS
        in TlbParserDefinition.PARENTHESES -> TlbColor.PARENTHESES
        SEMICOLUMN -> TlbColor.SEMICOLON
        NUMBER -> TlbColor.NUMBER
        HEX_TAG -> TlbColor.HEX_TAG
        BINARY_TAG -> TlbColor.BINARY_TAG
        PREDIFINED_TYPE -> TlbColor.IMPLICIT_FIELD_NAME
        IDENTIFIER -> TlbColor.IDENTIFIER
        CIRCUMFLEX, COLUMN, EQ -> TlbColor.OPERATION_SIGN
        in TlbParserDefinition.BUILTIN_TYPES -> TlbColor.IMPLICIT_FIELD_NAME
        else -> null
    }.let {
        pack(it?.textAttributesKey)
    }
}
