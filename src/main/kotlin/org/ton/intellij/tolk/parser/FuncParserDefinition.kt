package org.ton.intellij.tolk.parser

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
import org.ton.intellij.tolk.TolkFileElementType
import org.ton.intellij.tolk.doc.TolkDocCommentElementType
import org.ton.intellij.tolk.lexer.TolkLexer
import org.ton.intellij.tolk.psi.TOLK_COMMENTS
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkTokenType

class TolkParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = TolkLexer()

    override fun createParser(project: Project?): PsiParser = TolkParser()

    override fun getFileNodeType(): IFileElementType = TolkFileElementType

    override fun getCommentTokens(): TokenSet = TOLK_COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRING_LITERALS

    override fun createElement(node: ASTNode?): PsiElement =
        TolkElementTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider) =
        TolkFile(viewProvider)

    companion object {
        @JvmField
        val BLOCK_COMMENT = TolkTokenType("<BLOCK_COMMENT>")
        @JvmField
        val EOL_COMMENT = TolkTokenType("<EOL_COMMENT>")
        @JvmField
        val BLOCK_DOC_COMMENT = TolkTokenType("<BLOCK_DOC_COMMENT>")
        @JvmField
        val EOL_DOC_COMMENT = TolkDocCommentElementType("<EOL_DOC_COMMENT>")

        val PRIMITIVE_TYPES = TokenSet.create(
            TolkElementTypes.INT_KEYWORD,
            TolkElementTypes.CELL_KEYWORD,
            TolkElementTypes.SLICE_KEYWORD,
            TolkElementTypes.BUILDER_KEYWORD,
            TolkElementTypes.CONT_KEYWORD,
            TolkElementTypes.TUPLE_KEYWORD,
            TolkElementTypes.TRUE_KEYWORD,
            TolkElementTypes.FALSE_KEYWORD,
        )
        val WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE)
        val STRING_LITERALS = TokenSet.create(TolkElementTypes.STRING_LITERAL)

        val WHITE_SPACE_OR_COMMENT_BIT_SET = TokenSet.orSet(TOLK_COMMENTS, WHITESPACES)
        val ANNOTATIONS = TokenSet.create(
            TolkElementTypes.ANNOTATION,
            TolkElementTypes.METHOD_ID_KEYWORD,
            )
        val OPERATORS = TokenSet.create(
            TolkElementTypes.PLUS,
            TolkElementTypes.MINUS,
            TolkElementTypes.TIMES,
            TolkElementTypes.DIV,
            TolkElementTypes.MOD,
            TolkElementTypes.QUEST,
            TolkElementTypes.COLON,
            TolkElementTypes.EQ,
            TolkElementTypes.LT,
            TolkElementTypes.GT,
            TolkElementTypes.AND,
            TolkElementTypes.OR,
            TolkElementTypes.XOR,
            TolkElementTypes.EQEQ,
            TolkElementTypes.NEQ,
            TolkElementTypes.LEQ,
            TolkElementTypes.GEQ,
            TolkElementTypes.SPACESHIP,
            TolkElementTypes.LSHIFT,
            TolkElementTypes.RSHIFT,
            TolkElementTypes.RSHIFTR,
            TolkElementTypes.RSHIFTC,
            TolkElementTypes.DIVR,
            TolkElementTypes.DIVC,
            TolkElementTypes.MODR,
            TolkElementTypes.MODC,
            TolkElementTypes.DIVMOD,
            TolkElementTypes.PLUSLET,
            TolkElementTypes.MINUSLET,
            TolkElementTypes.TIMESLET,
            TolkElementTypes.DIVLET,
            TolkElementTypes.DIVCLET,
            TolkElementTypes.MODLET,
            TolkElementTypes.MODRLET,
            TolkElementTypes.MODCLET,
            TolkElementTypes.LSHIFTLET,
            TolkElementTypes.RSHIFTLET,
            TolkElementTypes.RSHIFTRLET,
            TolkElementTypes.RSHIFTCLET,
            TolkElementTypes.ANDLET,
            TolkElementTypes.ORLET,
            TolkElementTypes.XORLET,
            TolkElementTypes.MAPSTO
        )
    }
}
