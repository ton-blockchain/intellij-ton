package org.ton.intellij.func.ide.formatter

import com.intellij.formatting.*
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.psi.FuncElementTypes.*

class FuncFormatter : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val containingFile = formattingContext.containingFile
        val spacingBuilder = createSpacingBuilder(settings)
        val block = FuncFormattingBlock(
            node = containingFile.node,
            spacingBuilder = spacingBuilder,
            wrap = null,
            alignment = null,
            indent = Indent.getNoneIndent(),
            childIndent = Indent.getNoneIndent()
        )

        return FormattingModelProvider.createFormattingModelForPsiFile(containingFile, block, settings)
    }

    private fun createSpacingBuilder(codeStyleSettings: CodeStyleSettings): SpacingBuilder {
        return SpacingBuilder(codeStyleSettings, FuncLanguage)
            .after(TokenSet.create(LPAREN, LBRACK)).none()
            .after(
                TokenSet.create(
                    RETURN_KEYWORD,
                    VAR_KEYWORD,
                    REPEAT_KEYWORD,
                    DO_KEYWORD,
                    WHILE_KEYWORD,
                    UNTIL_KEYWORD,
                    TRY_KEYWORD,
                    CATCH_KEYWORD,
                    IF_KEYWORD,
                    IFNOT_KEYWORD,
                    THEN_KEYWORD,
                    ELSE_KEYWORD,
                    ELSEIF_KEYWORD,
                    ELSEIFNOT_KEYWORD,
                )
            ).spaces(1)
            .before(TokenSet.create(COMMA, SEMICOLON)).none()
            .after(TokenSet.create(COMMA, VAR_KEYWORD)).spaces(1)
            .after(TokenSet.create(STATEMENT, LBRACE)).lineBreakInCode()
            .before(RBRACE).lineBreakInCode()
            .after(RBRACE).spaces(1)
            .around(UNTIL_KEYWORD).spaces(1)
            .before(TokenSet.create(LINE_COMMENT, BLOCK_STATEMENT, ASM_BODY)).spaces(1)
            .aroundInside(
                TokenSet.create(
                    EQ,
                    PLUSLET,
                    MINUSLET,
                    TIMESLET,
                    DIVLET,
                    DIVRLET,
                    DIVCLET,
                    MODLET,
                    MODRLET,
                    MODCLET,
                    LSHIFTLET,
                    RSHIFTLET,
                    RSHIFTCLET,
                    RSHIFTRLET,
                    ANDLET,
                    ORLET,
                    XORLET
                ),
                ASSIGN_EXPRESSION
            ).spaces(1)
            .after(FORALL_KEYWORD).spaces(1)
            .around(MAPSTO).spaces(1)
            .betweenInside(LPAREN, MAPSTO, ASM_PARAMETERS).spaces(1)
            .betweenInside(
                TokenSet.create(
                    PRIMITIVE_TYPE,
                    TENSOR_TYPE,
                    TUPLE_TYPE,
                    TYPE_IDENTIFIER,
                    HOLE_TYPE
                ), TokenSet.create(IDENTIFIER, TILDE, DOT), FUNCTION
            ).spaces(1)
            .afterInside(TYPE, FUNCTION).spaces(1)
            .afterInside(TokenSet.create(DOT, TILDE), FUNCTION).none()
            .afterInside(IDENTIFIER, FUNCTION).none()
            .beforeInside(IDENTIFIER, FUNCTION_PARAMETER).spaces(1)
            .after(FUNCTION_PARAMETER).none()
            .beforeInside(TENSOR_EXPRESSION, CALL_EXPRESSION).none()
            .beforeInside(RPAREN, TokenSet.create(TENSOR_EXPRESSION, TENSOR_TYPE)).none()
            .beforeInside(RBRACK, TokenSet.create(TUPLE_EXPRESSION, TUPLE_TYPE)).none()
            .aroundInside(TokenSet.create(QUEST, COLON), TERNARY_EXPRESSION).spaces(1)
            .aroundInside(EQEQ, EQ_EXPRESSION).spaces(1)
            .aroundInside(LT, LT_EXPRESSION).spaces(1)
            .aroundInside(GT, GT_EXPRESSION).spaces(1)
            .aroundInside(LEQ, LEQ_EXPRESSION).spaces(1)
            .aroundInside(GEQ, GEQ_EXPRESSION).spaces(1)
            .aroundInside(NEQ, NEQ_EXPRESSION).spaces(1)
            .aroundInside(SPACESHIP, SPACESHIP_EXPRESSION).spaces(1)
            .aroundInside(LSHIFT, L_SHIFT_EXPRESSION).spaces(1)
            .aroundInside(RSHIFT, R_SHIFT_EXPRESSION).spaces(1)
            .aroundInside(RSHIFTC, R_SHIFT_C_EXPRESSION).spaces(1)
            .aroundInside(RSHIFTR, R_SHIFT_R_EXPRESSION).spaces(1)
            .aroundInside(PLUS, PLUS_EXPRESSION).spaces(1)
            .aroundInside(MINUS, MINUS_EXPRESSION).spaces(1)
            .aroundInside(OR, OR_EXPRESSION).spaces(1)
            .aroundInside(XOR, XOR_EXPRESSION).spaces(1)
            .aroundInside(TIMES, MUL_EXPRESSION).spaces(1)
            .aroundInside(DIV, DIV_EXPRESSION).spaces(1)
            .aroundInside(MOD, MOD_EXPRESSION).spaces(1)
            .aroundInside(DIVMOD, DIV_MOD_EXPRESSION).spaces(1)
            .aroundInside(DIVC, DIV_C_EXPRESSION).spaces(1)
            .aroundInside(DIVR, DIV_R_EXPRESSION).spaces(1)
            .aroundInside(MODC, MOD_C_EXPRESSION).spaces(1)
            .aroundInside(MODR, MOD_R_EXPRESSION).spaces(1)
            .aroundInside(AND, AND_EXPRESSION).spaces(1)
            .afterInside(MINUS, UNARY_MINUS_EXPRESSION).none()
            .afterInside(TILDE, INV_EXPRESSION).spaces(1)
            .aroundInside(TokenSet.create(REFERENCE_EXPRESSION), VAR_EXPRESSION).spaces(1)
            .afterInside(
                TokenSet.create(PRIMITIVE_TYPE_EXPRESSION, HOLE_TYPE_EXPRESSION, TENSOR_EXPRESSION),
                VAR_EXPRESSION
            ).spaces(1)
            .around(TokenSet.create(IMPURE_KEYWORD, INLINE_KEYWORD, INLINE_REF_KEYWORD, METHOD_ID_KEYWORD)).spaces(1)
    }
}
