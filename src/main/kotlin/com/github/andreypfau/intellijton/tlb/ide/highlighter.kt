package com.github.andreypfau.intellijton.tlb.ide

import com.github.andreypfau.intellijton.tlb.parser.TlbLexerAdapter
import com.github.andreypfau.intellijton.tlb.psi.TlbTypes.*
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

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
        NUMBER -> TlbColor.NUMBER
        HEX_TAG -> TlbColor.HEX_TAG
        BINARY_TAG -> TlbColor.BINARY_TAG
        in TlbParserDefinition.BUILTIN_TYPES -> TlbColor.TYPE
        else -> null
    }.let {
        pack(it?.textAttributesKey)
    }
}