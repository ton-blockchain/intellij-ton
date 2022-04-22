package org.ton.intellij.func.ide

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
import org.ton.intellij.func.lexer.FuncLexerAdapter
import org.ton.intellij.func.lexer.FuncParser
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.FuncTokenTypes.*
import org.ton.intellij.func.stub.FuncFileStub

class FuncParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = FuncLexerAdapter()
    override fun createParser(project: Project?): PsiParser = FuncParser()
    override fun getFileNodeType(): IFileElementType = FuncFileStub.Type
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createElement(node: ASTNode?): PsiElement = Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = FuncFile(viewProvider)

    companion object {
        val DOCUMENTATION = TokenSet.create(LINE_DOCUMENTATION, BLOCK_DOCUMENTATION)
        val COMMENTS = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT, *DOCUMENTATION.types)

        val KEYWORDS = TokenSet.create(
            RETURN, VAR, REPEAT, DO, WHILE, UNTIL, IF, IFNOT, THEN, ELSE, ELSEIF, ELSEIFNOT,
            EXTERN, GLOBAL, ASM, IMPURE, INLINE, INLINE_REF, AUTO_APPLY, METHOD_ID, OPERATOR,
            INFIXL, INFIXR, CONST, TILDE
        )
        val BRACES = TokenSet.create(LBRACE, RBRACE)
        val PARENTHESES = TokenSet.create(LPAREN, RPAREN)
        val BRACKETS = TokenSet.create(LBRACKET, RBRACKET)
        val TYPES = TokenSet.create(
            INT, CELL, SLICE, BUILDER, CONT, TUPLE, TYPE, FORALL, TRUE, FALSE
        )
        val OPERATORS = TokenSet.create(
            PLUS, MINUS, TIMES, DIVIDE, PERCENT, QUESTION, EQUALS, LESS, GREATER, AND, OR, CIRCUMFLEX,
            EQ, NEQ, NEQ, LEQ, GEQ, SPACESHIP, LSHIFT, RSHIFT, RSHIFTR, RSHIFTC, DIVR, DIVC, MODR, MODC, DIVMOD,
            PLUSLET, MINUSLET, TIMESLET, DIVLET, DIVRLET, DIVCLET, MODLET, MODRLET, MODCLET, LSHIFTLET, RSHIFTLET,
            RSHIFTRLET, RSHIFTCLET, ANDLET, ORLET, XORLET, MAPSTO
        )
    }
}