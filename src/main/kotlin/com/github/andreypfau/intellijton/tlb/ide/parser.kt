package com.github.andreypfau.intellijton.tlb.ide

import com.github.andreypfau.intellijton.tlb.TlbLanguage
import com.github.andreypfau.intellijton.tlb.parser.TlbLexerAdapter
import com.github.andreypfau.intellijton.tlb.parser.TlbParser
import com.github.andreypfau.intellijton.tlb.psi.TlbFile
import com.github.andreypfau.intellijton.tlb.psi.TlbTypes
import com.github.andreypfau.intellijton.tlb.psi.TlbTypes.*
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class TlbParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = TlbLexerAdapter()
    override fun createParser(project: Project?): PsiParser = TlbParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createElement(node: ASTNode?): PsiElement = Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = TlbFile(viewProvider)

    companion object {
        val FILE = IFileElementType(TlbLanguage)
        val DOCUMENTATION = TokenSet.create(LINE_DOCUMENTATION, BLOCK_DOCUMENTATION)
        val COMMENTS = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT, *DOCUMENTATION.types)

        val BRACES = TokenSet.create(LBRACE, RBRACE)
        val PARENTHESES = TokenSet.create(LPAREN, RPAREN)
        val BRACKETS = TokenSet.create(LBRACKET, RBRACKET)

        val UINT = TokenSet.create(
            TlbTypes.UINT, UINT1, UINT2, UINT4, UINT8, UINT16, UINT32, UINT64, UINT128, UINT256, UINT257
        )
        val BITS = TokenSet.create(
            TlbTypes.BITS, BITS1, BITS2, BITS4, BITS8, BITS16, BITS32, BITS64, BITS128, BITS256, BITS512, BITS1023
        )
        val BUILTIN_TYPES = TokenSet.create(
            TAG, DOUBLE_TAG, NAT_LESS, NAT_LEQ, ANY, CELL, INT, TYPE, *UINT.types, *BITS.types
        )
    }
}