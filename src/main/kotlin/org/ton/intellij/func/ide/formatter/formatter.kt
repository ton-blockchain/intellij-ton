package org.ton.intellij.func.ide.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import org.ton.intellij.TokenSet
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.psi.FuncTokenTypes.*

class FuncFormattingBlock(
        node: ASTNode,
        wrap: Wrap? = null,
        alignment: Alignment? = null,
        private val indent: Indent? = null,
        private val spacingBuilder: SpacingBuilder
) : AbstractBlock(node, wrap, alignment) {
    override fun getSpacing(child1: Block?, child2: Block): Spacing? = spacingBuilder.getSpacing(this, child1, child2)

    override fun isLeaf(): Boolean = myNode.firstChildNode != null

    override fun getIndent(): Indent? = indent

    override fun getChildIndent(): Indent? {
        if (node.elementType == BLOCK_STATEMENT) {
            return Indent.getNormalIndent()
        }
        return null
    }

    override fun buildChildren(): MutableList<Block> {
        val blocks = ArrayList<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType != TokenType.WHITE_SPACE) {
                val block = FuncFormattingBlock(
                        child,
                        Wrap.createWrap(WrapType.NONE, false),
                        null,
                        calcIndent(child),
                        spacingBuilder
                )
                blocks.add(block)
            }
            child = child.treeNext
        }
        return blocks
    }

    companion object {
        private fun calcIndent(child: ASTNode): Indent {
            val childType = child.elementType
            val parent = child.treeParent
            val parentType = parent?.elementType
            val result = when {
                parentType == BLOCK_STATEMENT && childType !in TokenSet(LBRACE, RBRACE) -> Indent.getNormalIndent()
                parentType in TokenSet(TENSOR_EXPRESSION, TENSOR_TYPE_EXPRESSION, PARAMETER_LIST) && childType !in TokenSet(LPAREN, RPAREN) -> Indent.getNormalIndent()
                parentType in TokenSet(TUPLE_EXPRESSION, TUPLE_TYPE_EXPRESSION) && childType !in TokenSet(LBRACKET, RBRACKET) -> Indent.getNormalIndent()
                else -> Indent.getNoneIndent()
            }
            return result
        }
    }
}

class FuncFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val codeStyleSettings = formattingContext.codeStyleSettings
        return FormattingModelProvider.createFormattingModelForPsiFile(
                formattingContext.containingFile,
                FuncFormattingBlock(
                        formattingContext.node,
                        Wrap.createWrap(WrapType.NONE, false),
                        null,
                        null,
                        createSpaceBuilder(codeStyleSettings)
                ),
                codeStyleSettings
        )
    }

    private fun createSpaceBuilder(codeStyleSettings: CodeStyleSettings): SpacingBuilder {
        return SpacingBuilder(codeStyleSettings, FuncLanguage)
            .after(TokenSet(LPAREN, LBRACE, LBRACKET)).none()
            .before(RPAREN).none()
            .before(RBRACE).none()
            .before(RBRACKET).none()
            .before(COMMA).none()
            .before(SEMICOLON).none()
            .around(TokenSet(IMPURE, INLINE, INLINE_REF, METHOD_ID)).spaces(1)
                .aroundInside(TokenSet(
                        EQUALS, PLUSLET, MINUSLET, TIMESLET, DIVLET, DIVRLET, DIVCLET, MODLET, MODRLET,
                        MODCLET, LSHIFTLET, RSHIFTLET, RSHIFTRLET, RSHIFTCLET, ANDLET, ORLET, XORLET
                ), ASSIGNMENT_EXPRESSION).spaces(1)
                .aroundInside(TokenSet(QUESTION, COLON), TERNARY_EXPRESSION).spaces(1)
                .aroundInside(TokenSet(EQ, LESS, GREATER, LEQ, GEQ, NEQ, SPACESHIP), EQUATION_EXPRESSION).spaces(1)
                .aroundInside(TokenSet(LSHIFT, RSHIFT, RSHIFTR, RSHIFTC), BITWISE_EXPRESSION).spaces(1)
                .aroundInside(TokenSet(TIMES, DIVIDE, PERCENT, DIVR, DIVC, MODR, MODC, DIVMOD, AND), EXPR_30).spaces(1)
                .after(RETURN).spaces(1)
                .after(COMMA).spaces(1)
                .afterInside(TILDE, MODIFYING_METHOD_CALL).none()
                .before(TokenSet(BLOCK_STATEMENT, ASM_FUNCTION_BODY)).spaces(1)
                .after(EXPRESSION_STATEMENT).lineBreakInCode()
                .between(TENSOR_EXPRESSION_ITEM, TENSOR_EXPRESSION_ITEM).spaces(1)
                .between(TUPLE_EXPRESSION_ITEM, TUPLE_EXPRESSION_ITEM).spaces(1)
                .between(TENSOR_TYPE_ITEM, TENSOR_TYPE_ITEM).spaces(1)
                .between(TUPLE_TYPE_ITEM, TUPLE_TYPE_ITEM).spaces(1)
                .between(LBRACE, RBRACE).lineBreakInCode()
                .between(TokenSet(FUNCTION), TokenSet(FUNCTION)).blankLines(1)
    }
}