package org.ton.intellij.fift.ide

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
import org.ton.intellij.fift.FiftLanguage
import org.ton.intellij.fift.lexer.FiftLexerAdapter
import org.ton.intellij.fift.parser.FiftParser
import org.ton.intellij.fift.psi.FiftFile
import org.ton.intellij.fift.psi.FiftTypes

class FiftParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = FiftLexerAdapter()
    override fun createParser(project: Project?): PsiParser = FiftParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createElement(node: ASTNode?): PsiElement = FiftTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = FiftFile(viewProvider)

    companion object {
        val FILE = IFileElementType(FiftLanguage)
        val DOCUMENTATION = TokenSet.create(FiftTypes.LINE_DOCUMENTATION, FiftTypes.BLOCK_DOCUMENTATION)
        val COMMENTS = TokenSet.create(FiftTypes.LINE_COMMENT, FiftTypes.BLOCK_COMMENT, *DOCUMENTATION.types)

        val BRACES = TokenSet.create(FiftTypes.LBRACE, FiftTypes.RBRACE)
        val PARENTHESES = TokenSet.create(FiftTypes.LPAREN, FiftTypes.RPAREN)
        val BRACKETS = TokenSet.create(FiftTypes.LBRACKET, FiftTypes.RBRACKET)
    }
}