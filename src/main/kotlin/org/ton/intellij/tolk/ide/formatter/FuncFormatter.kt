package org.ton.intellij.tolk.ide.formatter

import com.intellij.formatting.*
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.TokenSet.create
import org.ton.intellij.func.psi.FuncElementTypes.PRIMITIVE_TYPE_EXPRESSION
import org.ton.intellij.func.psi.FuncElementTypes.SPECIAL_APPLY_EXPRESSION
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.psi.TolkElementTypes.*

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
            .after(create(LPAREN, LBRACK)).none()
            .after(
                create(
                    RETURN_KEYWORD,
                    VAR_KEYWORD,
                    REPEAT_KEYWORD,
                    DO_KEYWORD,
                    WHILE_KEYWORD,
                    TRY_KEYWORD,
                    CATCH_KEYWORD,
                    IF_KEYWORD,
                    ELSE_KEYWORD,
                    IMPORT_KEYWORD,
                    FUN_KEYWORD,
                    CONST_KEYWORD,
                    TOLK_KEYWORD,
                    THROW_KEYWORD,
                )
            ).spaces(1)
            .afterInside(ASSERT_KEYWORD, ASSERT_STATEMENT).none()
            .beforeInside(THROW_KEYWORD, ASSERT_STATEMENT).spaces(1)
            .before(create(COMMA, SEMICOLON)).none()
            .after(create(COMMA, VAR_KEYWORD)).spaces(1)
            .after(create(STATEMENT, LBRACE)).lineBreakInCode()
            .before(RBRACE).lineBreakInCode()
            .after(RBRACE).spaces(1)
//            .around(UNTIL_KEYWORD).spaces(1)
            .before(create(BLOCK_STATEMENT, ASM_BODY)).spaces(1)
            .around(BINARY_OP).spaces(1)
//            .after(FORALL_KEYWORD).spaces(1)
            .around(MAPSTO).spaces(1)
            .betweenInside(LPAREN, MAPSTO, ASM_PARAMETERS).spaces(1)
            .betweenInside(
                create(
                    PRIMITIVE_TYPE,
                    TENSOR_TYPE,
                    TUPLE_TYPE,
                    TYPE_IDENTIFIER,
                ), create(IDENTIFIER, TILDE), FUNCTION
            ).spaces(1)
            .afterInside(TYPE_REFERENCE, FUNCTION).spaces(1)
            .afterInside(create(TILDE), FUNCTION).none()
            .afterInside(IDENTIFIER, FUNCTION).none()
            .beforeInside(IDENTIFIER, FUNCTION_PARAMETER).spaces(1)
            .after(FUNCTION_PARAMETER).none()
            .between(FUNCTION, FUNCTION).blankLines(1)
            .beforeInside(TENSOR_EXPRESSION, CALL_EXPRESSION).none()
            .beforeInside(UNIT_EXPRESSION, CALL_EXPRESSION).none()
            .beforeInside(TENSOR_EXPRESSION, DOT_EXPRESSION).none()
            .beforeInside(UNIT_EXPRESSION, DOT_EXPRESSION).none()
            .beforeInside(RPAREN, create(TENSOR_EXPRESSION, TENSOR_TYPE)).none()
            .beforeInside(RBRACK, create(TUPLE_EXPRESSION, TUPLE_TYPE)).none()
            .aroundInside(create(QUEST, COLON), TERNARY_EXPRESSION).spaces(1)
            .aroundInside(
                create(
                    EQ, PLUSLET, MINUSLET, TIMESLET, DIVLET, DIVCLET, DIVRLET, MODLET, MODCLET, MODRLET,
                    LSHIFTLET, RSHIFTLET, RSHIFTCLET, RSHIFTRLET, ANDLET, ORLET, XORLET, EQEQ, NEQ, LEQ,
                    GEQ, GT, LT, SPACESHIP, LSHIFT, RSHIFTR, RSHIFTC, MINUS, PLUS, OR, XOR, TIMES, DIV, MOD,
                    DIVMOD, DIVC, DIVR, MODR, MODC, AND
                ), BIN_EXPRESSION
            ).spaces(1)
            .afterInside(create(EXCL, TILDE, MINUS, PLUS), PREFIX_EXPRESSION).none()
            .aroundInside(
                create(LPAREN, RPAREN),
                create(
                    ASSERT_STATEMENT,
                    DO_STATEMENT,
                    IF_STATEMENT,
                    ANNOTATION_PARAMS,
                    ARGUMENT_LIST,
                    VAR_TENSOR,
                    CATCH,
                )
            ).spaces(0)
//            .around(TokenSet.create(IMPURE_KEYWORD, INLINE_KEYWORD, INLINE_REF_KEYWORD, METHOD_ID_KEYWORD)).spaces(1)
    }
}
