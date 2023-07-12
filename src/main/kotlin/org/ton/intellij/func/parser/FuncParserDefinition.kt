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
import org.ton.intellij.func.lexer.FuncLexer
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncFile

class FuncParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = FuncLexer()

    override fun createParser(project: Project?): PsiParser = FuncParser()

    override fun getFileNodeType(): IFileElementType = FuncFileElementType

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRING_LITERALS

    override fun createElement(node: ASTNode?): PsiElement =
        FuncElementTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider) =
        FuncFile(viewProvider)

    companion object {
        val MACRO = TokenSet.create(FuncElementTypes.INCLUDE_MACRO, FuncElementTypes.PRAGMA_MACRO)
        val KEYWORDS = TokenSet.create(
            FuncElementTypes.RETURN_KEYWORD,
            FuncElementTypes.VAR_KEYWORD,
            FuncElementTypes.REPEAT_KEYWORD,
            FuncElementTypes.DO_KEYWORD,
            FuncElementTypes.WHILE_KEYWORD,
            FuncElementTypes.UNTIL_KEYWORD,
            FuncElementTypes.TRY_KEYWORD,
            FuncElementTypes.CATCH_KEYWORD,
            FuncElementTypes.IF_KEYWORD,
            FuncElementTypes.IFNOT_KEYWORD,
            FuncElementTypes.THEN_KEYWORD,
            FuncElementTypes.ELSE_KEYWORD,
            FuncElementTypes.ELSEIF_KEYWORD,
            FuncElementTypes.ELSEIFNOT_KEYWORD,

            FuncElementTypes.INT_KEYWORD,
            FuncElementTypes.CELL_KEYWORD,
            FuncElementTypes.SLICE_KEYWORD,
            FuncElementTypes.BUILDER_KEYWORD,
            FuncElementTypes.CONT_KEYWORD,
            FuncElementTypes.TUPLE_KEYWORD,
            FuncElementTypes.TYPE_KEYWORD,
            FuncElementTypes.FORALL_KEYWORD,
            FuncElementTypes.TRUE_KEYWORD,
            FuncElementTypes.FALSE_KEYWORD,

            FuncElementTypes.EXTERN_KEYWORD,
            FuncElementTypes.GLOBAL_KEYWORD,
            FuncElementTypes.ASM_KEYWORD,
            FuncElementTypes.IMPURE_KEYWORD,
            FuncElementTypes.INLINE_KEYWORD,
            FuncElementTypes.INLINE_REF_KEYWORD,
            FuncElementTypes.AUTO_APPLY_KEYWORD,
            FuncElementTypes.METHOD_ID_KEYWORD,
            FuncElementTypes.OPERATOR_KEYWORD,
            FuncElementTypes.INFIX_KEYWORD,
            FuncElementTypes.INFIXL_KEYWORD,
            FuncElementTypes.INFIXR_KEYWORD,
            FuncElementTypes.CONST_KEYWORD,
        )
        val PRIMITIVE_TYPES = TokenSet.create(
            FuncElementTypes.INT_KEYWORD,
            FuncElementTypes.CELL_KEYWORD,
            FuncElementTypes.SLICE_KEYWORD,
            FuncElementTypes.BUILDER_KEYWORD,
            FuncElementTypes.CONT_KEYWORD,
            FuncElementTypes.TUPLE_KEYWORD,
        )
        val WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE)
        val COMMENTS = TokenSet.create(FuncElementTypes.LINE_COMMENT, FuncElementTypes.BLOCK_COMMENT, FuncElementTypes.DOC_COMMENT)
        val STRING_LITERALS = TokenSet.create(FuncElementTypes.STRING_LITERAL)

        val WHITE_SPACE_OR_COMMENT_BIT_SET = TokenSet.orSet(COMMENTS, WHITESPACES);

    }
}
