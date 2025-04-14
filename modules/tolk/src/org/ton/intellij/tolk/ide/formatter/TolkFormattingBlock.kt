package org.ton.intellij.tolk.ide.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.DOC_COMMENT
import org.ton.intellij.tolk.psi.TolkElementTypes.*
import java.util.*

abstract class AbstractTolkBlock(
    node: ASTNode,
    val spacingBuilder: SpacingBuilder,
    wrap: Wrap? = null,
    alignment: Alignment? = null,
    private val indent: Indent? = Indent.getNoneIndent(),
    private val childIndent: Indent? = null,
) : AbstractBlock(node, wrap, alignment) {
    override fun getSpacing(child1: Block?, child2: Block): Spacing? = spacingBuilder.getSpacing(this, child1, child2)

    override fun isLeaf(): Boolean = myNode.firstChildNode == null

    override fun getIndent(): Indent? = indent

    override fun getChildIndent(): Indent? = childIndent

    companion object {
        fun block(
            node: ASTNode,
            spacingBuilder: SpacingBuilder,
            wrap: Wrap? = null,
            indent: Indent? = null,
            buildChildren: () -> List<Block> = { emptyList() },
        ) =
            object : AbstractTolkBlock(node, spacingBuilder, wrap = wrap, indent = indent) {
                override fun buildChildren(): List<Block> {
                    return buildChildren.invoke()
                }
            }
    }
}

class TolkFormattingBlock(
    node: ASTNode,
    spacingBuilder: SpacingBuilder,
    wrap: Wrap? = null,
    alignment: Alignment? = null,
    indent: Indent? = null,
    childIndent: Indent? = null,
) : AbstractTolkBlock(node, spacingBuilder, wrap, alignment, indent, childIndent) {
    override fun buildChildren(): MutableList<Block> {
        val childrenBlocks = LinkedList<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            val block = createBlock(child)
            if (block != null) {
                childrenBlocks.add(block)
            }
            child = child.treeNext
        }
        return childrenBlocks
    }

    private fun createBlock(node: ASTNode): ASTBlock? {
        if (node.elementType == TokenType.WHITE_SPACE) return null

        val indent = calcIndent(node) ?: return null
        val childIndent =
            when (node.elementType) {
                BLOCK_STATEMENT -> Indent.getNormalIndent()
                DOC_COMMENT -> Indent.getNoneIndent()
                else -> Indent.getNormalIndent()
            }
        val wrap = calcWrap(node)

        return TolkFormattingBlock(
            node,
            spacingBuilder,
            wrap,
            null,
            indent,
            childIndent
        )
    }

    private fun calcIndent(child: ASTNode): Indent? {
        val type = child.elementType
        val parent = child.treeParent
        val parentType = parent.elementType
        when (parentType) {
            MATCH_EXPRESSION -> if (type != MATCH_KEYWORD) return indentIfNotBrace(child)
            PARAMETER_LIST, BLOCK_STATEMENT, STRUCT_EXPRESSION_BODY, STRUCT_BODY -> return indentIfNotBrace(child)
            DOT_EXPRESSION, TERNARY_EXPRESSION -> if (parent.firstChildNode != child) return Indent.getNormalIndent()
            VAR_TENSOR, TENSOR_EXPRESSION, PAREN_EXPRESSION, TENSOR_TYPE_EXPRESSION, PAREN_TYPE_EXPRESSION, ARGUMENT_LIST -> if (type != LPAREN && type != RPAREN) return Indent.getNormalIndent()
            VAR_TUPLE, TUPLE_TYPE_EXPRESSION, TUPLE_EXPRESSION -> if (type != LBRACK && type != RBRACK) return Indent.getNormalIndent()
            FUNCTION_BODY -> if (type == BUILTIN_KEYWORD || type == ASM_DEFINITION) return Indent.getNormalIndent()
        }
        // todo: check if intent work normally
//        if (type == PRIMITIVE_TYPE_EXPRESSION || type == HOLE_TYPE_EXPRESSION) return Indent.getNoneIndent()
        return Indent.getNoneIndent()
    }

    private fun calcWrap(child: ASTNode): Wrap? {
        return null
    }

    private val BRACES_TOKEN_SET = TokenSet.create(
        LBRACE,
        RBRACE,
        LBRACK,
        RBRACK,
        LPAREN,
        RPAREN
    )

    private fun indentIfNotBrace(child: ASTNode): Indent =
        if (BRACES_TOKEN_SET.contains(child.elementType)) Indent.getNoneIndent()
        else Indent.getNormalIndent()
}
