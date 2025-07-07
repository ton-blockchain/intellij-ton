package org.ton.intellij.tolk.ide.formatter

import com.intellij.formatting.*
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.psi.TolkElementTypes.*
import org.ton.intellij.util.tokenSetOf

class TolkFormatter : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val containingFile = formattingContext.containingFile
        val spacingBuilder = createSpacingBuilder(settings)
        val block = TolkFormattingBlock(
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
        return SpacingBuilder(codeStyleSettings, TolkLanguage)
            .after(TokenSet.create(LPAREN, LBRACK)).none()
            .around(EQ).spaces(1)

            .aroundInside(IDENTIFIER, ANNOTATION).none()
            .after(TokenSet.create(ANNOTATION, GET_KEYWORD, FUN_KEYWORD, RETURN_TYPE)).spaces(1)
            .beforeInside(RETURN_TYPE, FUNCTION).none()
            .afterInside(COLON, RETURN_TYPE).spaces(1)
            .after(FUNCTION_RECEIVER).none()
            .around(FUNCTION_BODY).spaces(1)

            .beforeInside(TokenSet.create(IDENTIFIER, STRUCT_CONSTRUCTOR_TAG), STRUCT).spaces(1)
            .around(STRUCT_BODY).spaces(1)

            .afterInside(IDENTIFIER, TokenSet.create(STRUCT_FIELD, STRUCT_EXPRESSION_FIELD)).none()
            .afterInside(COLON, TokenSet.create(STRUCT_FIELD, STRUCT_EXPRESSION_FIELD)).spaces(1)
            .before(STRUCT_FIELD).lineBreakInCode()
            .after(
                TokenSet.create(
                    RETURN_KEYWORD,
                    VAR_KEYWORD,
                    REPEAT_KEYWORD,
                    DO_KEYWORD,
                    WHILE_KEYWORD,
                    TRY_KEYWORD,
                    CATCH_KEYWORD,
                    IF_KEYWORD,
                    ELSE_KEYWORD,
                    MATCH_KEYWORD
                )
            ).spaces(1)
            .before(TokenSet.create(COMMA, SEMICOLON)).none()
            .after(TokenSet.create(COMMA, VAR_KEYWORD)).spaces(1)
            .after(TokenSet.create(STATEMENT, LBRACE)).lineBreakInCode()
            .before(RBRACE).lineBreakInCode()
            .after(RBRACE).spaces(1)
            .before(TokenSet.create(BLOCK_STATEMENT, ASM_BODY)).spaces(1)
            .around(BINARY_OP).spaces(1)
            .around(TokenSet.create(MAPSTO, ARROW)).spaces(1)
            .betweenInside(LPAREN, MAPSTO, ASM_PARAMETERS).spaces(1)
            .aroundInside(OR, UNION_TYPE_EXPRESSION).spaces(1)
            .betweenInside(
                TokenSet.create(
                    TENSOR_TYPE_EXPRESSION,
                    TUPLE_TYPE_EXPRESSION,
                    REFERENCE_TYPE_EXPRESSION,
                ), TokenSet.create(IDENTIFIER, TILDE), FUNCTION
            ).spaces(1)
            .afterInside(TYPE_EXPRESSION, FUNCTION).spaces(1)
            .afterInside(TokenSet.create(TILDE), FUNCTION).none()
            .afterInside(IDENTIFIER, FUNCTION).none()
            .between(FUNCTION, FUNCTION).blankLines(1)
            .after(ASSERT_KEYWORD).spaces(1)
            .beforeInside(THROW_STATEMENT, ASSERT_STATEMENT).spaces(1)
            .beforeInside(TENSOR_EXPRESSION, CALL_EXPRESSION).none()
            .beforeInside(UNIT_EXPRESSION, CALL_EXPRESSION).none()
            .beforeInside(TENSOR_EXPRESSION, DOT_EXPRESSION).none()
            .beforeInside(UNIT_EXPRESSION, DOT_EXPRESSION).none()
            .before(RPAREN).none()
            .beforeInside(RBRACK, TokenSet.create(TUPLE_EXPRESSION, TUPLE_TYPE_EXPRESSION)).none()
            .aroundInside(TokenSet.create(QUEST, COLON), TERNARY_EXPRESSION).spaces(1)
            .beforeInside(GT, TYPE_PARAMETER_LIST).none()
            .aroundInside(LT, TYPE_PARAMETER_LIST).none()
            .before(TYPE_PARAMETER_LIST).none()
            .beforeInside(DEFAULT_TYPE_PARAMETER, TYPE_PARAMETER).spaces(1)
            .aroundInside(
                tokenSetOf(
                    EQ, PLUSLET, MINUSLET, TIMESLET, DIVLET, DIVCLET, DIVRLET, MODLET, MODCLET, MODRLET,
                    LSHIFTLET, RSHIFTLET, RSHIFTCLET, RSHIFTRLET, ANDLET, ORLET, XORLET, EQEQ, NEQ, LEQ,
                    GEQ, GT, LT, SPACESHIP, LSHIFT, RSHIFTR, RSHIFTC, MINUS, PLUS, OR, XOR, TIMES, DIV, MOD,
                    DIVMOD, DIVC, DIVR, MODR, MODC, AND
                ), BIN_EXPRESSION
            ).spaces(1)
            .afterInside(tokenSetOf(EXCL, TILDE, MINUS, PLUS), PREFIX_EXPRESSION).none()
    }
}
