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

    // TODO: remake space builder for new tolk syntax
    private fun createSpacingBuilder(codeStyleSettings: CodeStyleSettings): SpacingBuilder {
        return SpacingBuilder(codeStyleSettings, TolkLanguage)
            .after(TokenSet.create(LPAREN, LBRACK)).none()
            .around(EQ).spaces(1)

            .aroundInside(IDENTIFIER, ANNOTATION).none()
            .after(TokenSet.create(ANNOTATION, GET_KEYWORD, FUN_KEYWORD, FUNCTION_RECEIVER, RETURN_TYPE, PARAMETER_LIST)).spaces(1)
            .around(FUNCTION_BODY).spaces(1)

            .before(STRUCT_CONSTRUCTOR_TAG).none()
            .beforeInside(IDENTIFIER, STRUCT).spaces(1)
            .around(STRUCT_BODY).spaces(1)

            .afterInside(IDENTIFIER, STRUCT_FIELD).none()
            .afterInside(COLON, STRUCT_FIELD).spaces(1)

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
                )
            ).spaces(1)
            .before(TokenSet.create(COMMA, SEMICOLON)).none()
            .after(TokenSet.create(COMMA, VAR_KEYWORD)).spaces(1)
            .after(TokenSet.create(STATEMENT, LBRACE)).lineBreakInCode()
            .before(RBRACE).lineBreakInCode()
            .after(RBRACE).spaces(1)
//            .around(UNTIL_KEYWORD).spaces(1)
            .before(TokenSet.create(BLOCK_STATEMENT, ASM_BODY)).spaces(1)
            .around(BINARY_OP).spaces(1)
//            .after(FORALL_KEYWORD).spaces(1)
            .around(MAPSTO).spaces(1)
            .betweenInside(LPAREN, MAPSTO, ASM_PARAMETERS).spaces(1)
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
//            .around(TokenSet.create(IMPURE_KEYWORD, INLINE_KEYWORD, INLINE_REF_KEYWORD, METHOD_ID_KEYWORD)).spaces(1)
    }
}
