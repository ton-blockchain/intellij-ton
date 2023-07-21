package org.ton.intellij.func.ide.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.func.psi.FuncElementTypes.*
import org.ton.intellij.func.psi.FuncExpression
import java.util.*

class FuncFormattingBlock(
    node: ASTNode,
    private val spacingBuilder: SpacingBuilder,
    wrap: Wrap? = null,
    alignment: Alignment? = null,
    private val indent: Indent? = null,
    private val childIndent: Indent? = null,
) : AbstractBlock(node, wrap, alignment) {
    override fun getSpacing(child1: Block?, child2: Block): Spacing? = spacingBuilder.getSpacing(this, child1, child2)

    override fun isLeaf(): Boolean = myNode.firstChildNode != null

    override fun getIndent(): Indent? = indent

    override fun getChildIndent(): Indent? = childIndent

    override fun buildChildren(): MutableList<Block> {
        val childrenBlocks = LinkedList<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType != TokenType.WHITE_SPACE) {
                val indent = calcIndent(child)
                if (indent != null) {
                    val childIndent = if (child.elementType == BLOCK_STATEMENT) {
                        Indent.getNormalIndent()
                    } else null
                    val block = FuncFormattingBlock(
                        child,
                        spacingBuilder,
                        null,
                        null,
                        indent,
                        childIndent
                    )
                    childrenBlocks.add(block)
                }
            }
            child = child.treeNext
        }
        return childrenBlocks
    }

    private fun calcIndent(child: ASTNode): Indent? {
        val type = child.elementType
        val parentType = child.treeParent.elementType
        if (parentType == BLOCK_STATEMENT) return indentIfNotBrace(child)
        if (parentType == TENSOR_EXPRESSION && type != LPAREN && type != RPAREN) return Indent.getNormalIndent()
        if (type == PRIMITIVE_TYPE_EXPRESSION || type == HOLE_TYPE_EXPRESSION) return Indent.getNoneIndent()
        if (child.psi is FuncExpression) return Indent.getContinuationWithoutFirstIndent()
        return Indent.getNoneIndent()
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
