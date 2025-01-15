package org.ton.intellij.tact.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import org.ton.intellij.tact.psi.TactElementTypes

class TactFormatterBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val indent: Indent? = null,
    private val spacingBuilder: SpacingBuilder
) : AbstractBlock(node, null, null) {
    private val childIndent = when (node.elementType) {
        TactElementTypes.CONTRACT_BODY,
        TactElementTypes.TRAIT_BODY,
        TactElementTypes.BLOCK_FIELDS,
        TactElementTypes.BLOCK -> Indent.getNormalIndent()

        else -> Indent.getNoneIndent()
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? = spacingBuilder.getSpacing(this, child1, child2)

    override fun getIndent(): Indent? = indent

    override fun buildChildren(): List<TactFormatterBlock> {
        val blocks = ArrayList<TactFormatterBlock>()
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType != TokenType.WHITE_SPACE) {
                blocks.add(TactFormatterBlock(child, null, null, computeIndent(child), spacingBuilder))
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getChildIndent(): Indent? = childIndent

    override fun isLeaf(): Boolean = myNode.firstChildNode == null

    fun computeIndent(child: ASTNode): Indent? {
        val parentType = node.elementType
        val childType = child.elementType

        return when (parentType) {
            TactElementTypes.CONTRACT_BODY,
            TactElementTypes.TRAIT_BODY,
            TactElementTypes.BLOCK_FIELDS,
            TactElementTypes.BLOCK -> {
                when (childType) {
                    TactElementTypes.LBRACE, TactElementTypes.RBRACE -> Indent.getNoneIndent()
                    else -> Indent.getNormalIndent()
                }
            }

            else -> Indent.getNoneIndent()
        }
    }
}
