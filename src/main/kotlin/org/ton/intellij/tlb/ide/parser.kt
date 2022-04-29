package org.ton.intellij.tlb.ide

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
import org.ton.intellij.tlb.TlbLanguage
import org.ton.intellij.tlb.parser.TlbLexerAdapter
import org.ton.intellij.tlb.parser.TlbParser
import org.ton.intellij.tlb.psi.TlbFile
import org.ton.intellij.tlb.psi.TlbTypes.*

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
        val BUILTIN_TYPES = TokenSet.create(
            TAG, DOUBLE_TAG, NAT_LESS, NAT_LEQ
        )
        val INBUILT_TYPE_NAMES = buildSet {
            add("Any")
            add("Cell")
            add("Type")
            add("int")
            add("uint")
            add("bits")
            repeat(2056) {
                add("int$it")
            }
            repeat(2057) {
                add("uint$it")
            }
            repeat(1023) {
                add("bits$it")
            }
            BUILTIN_TYPES.types.forEach { builtinType ->
                add(builtinType.toString())
            }
        }
    }
}