package org.ton.intellij.func.ide.formatter

import com.intellij.formatting.*
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.psi.FuncElementTypes.*
import org.ton.intellij.util.tokenSetOf

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
            .before(TokenSet.create(BLOCK_STATEMENT, ASM_BODY)).spaces(1)
            .around(BINARY_OP).spaces(1)
            .after(FORALL_KEYWORD).spaces(1)
            .around(MAPSTO).spaces(1)
            .afterInside(tokenSetOf(PRIMITIVE_TYPE_EXPRESSION, HOLE_TYPE_EXPRESSION), APPLY_EXPRESSION).spaces(1)
            .betweenInside(LPAREN, MAPSTO, ASM_PARAMETERS).spaces(1)
            .betweenInside(
                TokenSet.create(
                    PRIMITIVE_TYPE,
                    TENSOR_TYPE,
                    TUPLE_TYPE,
                    TYPE_IDENTIFIER,
                    HOLE_TYPE
                ), TokenSet.create(IDENTIFIER, TILDE), FUNCTION
            ).spaces(1)
            .afterInside(TYPE_REFERENCE, FUNCTION).spaces(1)
            .afterInside(TokenSet.create(TILDE), FUNCTION).none()
            .afterInside(IDENTIFIER, FUNCTION).none()
            .beforeInside(IDENTIFIER, FUNCTION_PARAMETER).spaces(1)
            .after(FUNCTION_PARAMETER).none()
            .beforeInside(TENSOR_EXPRESSION, APPLY_EXPRESSION).none()
            .beforeInside(UNIT_EXPRESSION, APPLY_EXPRESSION).none()
            .beforeInside(TENSOR_EXPRESSION, SPECIAL_APPLY_EXPRESSION).none()
            .beforeInside(UNIT_EXPRESSION, SPECIAL_APPLY_EXPRESSION).none()
            .beforeInside(RPAREN, TokenSet.create(TENSOR_EXPRESSION, TENSOR_TYPE)).none()
            .beforeInside(RBRACK, TokenSet.create(TUPLE_EXPRESSION, TUPLE_TYPE)).none()
            .aroundInside(TokenSet.create(QUEST, COLON), TERNARY_EXPRESSION).spaces(1)
            .aroundInside(
                tokenSetOf(
                    EQ, PLUSLET, MINUSLET, TIMESLET, DIVLET, DIVCLET, DIVRLET, MODLET, MODCLET, MODRLET,
                    LSHIFTLET, RSHIFTLET, RSHIFTCLET, RSHIFTRLET, ANDLET, ORLET, XORLET, EQEQ, NEQ, LEQ,
                    GEQ, GT, LT, SPACESHIP, LSHIFT, RSHIFTR, RSHIFTC, MINUS, PLUS, OR, XOR, TIMES, DIV, MOD,
                    DIVMOD, DIVC, DIVR, MODR, MODC, AND
                ), BIN_EXPRESSION
            ).spaces(1)
            .afterInside(MINUS, UNARY_MINUS_EXPRESSION).none()
            .afterInside(TILDE, INV_EXPRESSION).spaces(1)
            .around(TokenSet.create(IMPURE_KEYWORD, INLINE_KEYWORD, INLINE_REF_KEYWORD, METHOD_ID_KEYWORD)).spaces(1)
    }
}
