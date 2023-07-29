package org.ton.intellij.func.ide.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.func.psi.FuncElementTypes.*
import java.util.*

abstract class AbstractFuncBlock(
    node: ASTNode,
    val spacingBuilder: SpacingBuilder,
    wrap: Wrap? = null,
    alignment: Alignment? = null,
    private val indent: Indent? = null,
    private val childIndent: Indent? = null,
) : AbstractBlock(node, wrap, alignment) {
    override fun getSpacing(child1: Block?, child2: Block): Spacing? = spacingBuilder.getSpacing(this, child1, child2)

    override fun isLeaf(): Boolean = myNode.firstChildNode != null

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
            object : AbstractFuncBlock(node, spacingBuilder, wrap = wrap, indent = indent) {
                override fun buildChildren(): List<Block> {
                    return buildChildren.invoke()
                }
            }
    }
}

class FuncFormattingBlock(
    node: ASTNode,
    spacingBuilder: SpacingBuilder,
    wrap: Wrap? = null,
    alignment: Alignment? = null,
    indent: Indent? = null,
    childIndent: Indent? = null,
) : AbstractFuncBlock(node, spacingBuilder, wrap, alignment, indent, childIndent) {
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
        val childIndent = if (node.elementType == BLOCK_STATEMENT) Indent.getNormalIndent() else null
        val wrap = calcWrap(node)

        return FuncFormattingBlock(
            node,
            spacingBuilder,
            wrap,
            null,
            indent,
            childIndent
        )
    }

    private fun Int.example() = this

    private fun a() {
        val a = 1
        a.example().example().example().example()

        a.example()
            .example()
            .example()
            .example()

    }

//    private fun createChainingCallBlock(node: FuncQualifiedExpression): ASTBlock {
//        return object : AbstractFuncBlock(node.node, spacingBuilder) {
//            override fun buildChildren(): List<Block> = buildList {
//                val expressions = node.expressionList
//
//                val first = expressions.firstOrNull()
//                if (first is FuncQualifiedExpression) {
//                    add(createChainingCallBlock(first))
//                } else if (first != null) {
//                    createBlock(first.node)?.let {
//                        add(it)
//                    }
//                }
//
//                add(
//                    block(node.node, spacingBuilder, indent = Indent.getNormalIndent(), wrap = Wrap.createChildWrap(Wrap.createWrap(WrapType.NONE, false), WrapType.NORMAL, false)) {
//                        buildList {
//                            node.dot?.node?.let {
//                                add(block(it, spacingBuilder, indent= Indent.getNoneIndent()))
//                            }
//                            expressions.getOrNull(1)?.node?.let {
//                                createBlock(it)
//                            }?.let {
//                                add(it)
//                            }
//                        }
//                    }
//                )
//            }
//        }
//    }

    private fun calcIndent(child: ASTNode): Indent? {
        val type = child.elementType
        val parent = child.treeParent
        val parentType = parent.elementType
        if (parentType == BLOCK_STATEMENT) return indentIfNotBrace(child)
        if (parentType == TENSOR_EXPRESSION && type != LPAREN && type != RPAREN) return Indent.getNormalIndent()
        if (type == PRIMITIVE_TYPE_EXPRESSION || type == HOLE_TYPE_EXPRESSION) return Indent.getNoneIndent()
        if (parentType == QUALIFIED_EXPRESSION && (type == DOT || type == TILDE)) return Indent.getContinuationWithoutFirstIndent()
//        if (parentType == ASSIGN_EXPRESSION) return Indent.getContinuationWithoutFirstIndent(true)
//        if (parentType == QUALIFIED_EXPRESSION && type != QUALIFIED_EXPRESSION) {
//            return Indent.getContinuationWithoutFirstIndent()
//        }
//        if (parentType == QUALIFIED_EXPRESSION && type == QUALIFIED_EXPRESSION) return Indent.getNormalIndent()
//        if (child.psi is FuncExpression) return Indent.getContinuationWithoutFirstIndent()
        return Indent.getNoneIndent()
    }

    private fun calcWrap(child: ASTNode): Wrap? {
        val type = child.elementType
        val parentType = child.treeParent.elementType
        if ((type == DOT || type == TILDE) && parentType == QUALIFIED_EXPRESSION) return Wrap.createWrap(
            WrapType.NORMAL,
            false
        )
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
