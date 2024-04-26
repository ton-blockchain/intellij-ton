package org.ton.intellij.func.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.func.FuncFileElementType
import org.ton.intellij.func.doc.FuncDocCommentElementType
import org.ton.intellij.func.lexer.FuncLexer
import org.ton.intellij.func.psi.FUNC_COMMENTS
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.FuncTokenType

class FuncParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = FuncLexer()

    override fun createParser(project: Project?): PsiParser = FuncParser()

    override fun getFileNodeType(): IFileElementType = FuncFileElementType

    override fun getCommentTokens(): TokenSet = FUNC_COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRING_LITERALS

    override fun createElement(node: ASTNode?): PsiElement =
        FuncElementTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider) =
        FuncFile(viewProvider)

    companion object {
        @JvmField
        val BLOCK_COMMENT = FuncTokenType("<BLOCK_COMMENT>")
        @JvmField
        val EOL_COMMENT = FuncTokenType("<EOL_COMMENT>")
        @JvmField
        val BLOCK_DOC_COMMENT = FuncTokenType("<BLOCK_DOC_COMMENT>")
        @JvmField
        val EOL_DOC_COMMENT = FuncDocCommentElementType("<EOL_DOC_COMMENT>")

        val PRIMITIVE_TYPES = TokenSet.create(
            FuncElementTypes.INT_KEYWORD,
            FuncElementTypes.CELL_KEYWORD,
            FuncElementTypes.SLICE_KEYWORD,
            FuncElementTypes.BUILDER_KEYWORD,
            FuncElementTypes.CONT_KEYWORD,
            FuncElementTypes.TUPLE_KEYWORD,
            FuncElementTypes.TRUE_KEYWORD,
            FuncElementTypes.FALSE_KEYWORD,
        )
        val WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE)
        val STRING_LITERALS = TokenSet.create(FuncElementTypes.STRING_LITERAL)

        val WHITE_SPACE_OR_COMMENT_BIT_SET = TokenSet.orSet(FUNC_COMMENTS, WHITESPACES)
        val OPERATORS = TokenSet.create(
            FuncElementTypes.PLUS,
            FuncElementTypes.MINUS,
            FuncElementTypes.TIMES,
            FuncElementTypes.DIV,
            FuncElementTypes.MOD,
            FuncElementTypes.QUEST,
            FuncElementTypes.COLON,
            FuncElementTypes.EQ,
            FuncElementTypes.LT,
            FuncElementTypes.GT,
            FuncElementTypes.AND,
            FuncElementTypes.OR,
            FuncElementTypes.XOR,
            FuncElementTypes.EQEQ,
            FuncElementTypes.NEQ,
            FuncElementTypes.LEQ,
            FuncElementTypes.GEQ,
            FuncElementTypes.SPACESHIP,
            FuncElementTypes.LSHIFT,
            FuncElementTypes.RSHIFT,
            FuncElementTypes.RSHIFTR,
            FuncElementTypes.RSHIFTC,
            FuncElementTypes.DIVR,
            FuncElementTypes.DIVC,
            FuncElementTypes.MODR,
            FuncElementTypes.MODC,
            FuncElementTypes.DIVMOD,
            FuncElementTypes.PLUSLET,
            FuncElementTypes.MINUSLET,
            FuncElementTypes.TIMESLET,
            FuncElementTypes.DIVLET,
            FuncElementTypes.DIVCLET,
            FuncElementTypes.MODLET,
            FuncElementTypes.MODRLET,
            FuncElementTypes.MODCLET,
            FuncElementTypes.LSHIFTLET,
            FuncElementTypes.RSHIFTLET,
            FuncElementTypes.RSHIFTRLET,
            FuncElementTypes.RSHIFTCLET,
            FuncElementTypes.ANDLET,
            FuncElementTypes.ORLET,
            FuncElementTypes.XORLET,
            FuncElementTypes.MAPSTO
        )
    }
}
