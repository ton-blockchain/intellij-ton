package org.ton.intellij.tolk.ide.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.DOC_BLOCK_COMMENT
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.DOC_EOL_COMMENT
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

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        if (child1 != null) {
            val node1 = (child1 as? AbstractTolkBlock)?.node

            if (node1?.elementType == ASSERT_KEYWORD && myNode.elementType == ASSERT_STATEMENT) {
                val assertStatement = myNode
                val hasThrowStatement = assertStatement.getChildren(TokenSet.create(THROW_STATEMENT)).isNotEmpty()
                val hasComma = assertStatement.getChildren(TokenSet.create(COMMA)).isNotEmpty()

                return if (hasThrowStatement && !hasComma) {
                    // assert (...) throw ...
                    Spacing.createSpacing(1, 1, 0, false, 0)
                } else {
                    // assert(..., ...)
                    Spacing.createSpacing(0, 0, 0, false, 0)
                }
            }
        }

        return spacingBuilder.getSpacing(this, child1, child2)
    }
    override fun buildChildren(): MutableList<Block> {
        if (myNode is TolkDocComment) {
            return mutableListOf()
        }

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
                ANNOTATION -> Indent.getNoneIndent()
                DOC_BLOCK_COMMENT, DOC_EOL_COMMENT -> Indent.getNoneIndent()
                FUNCTION, STRUCT, ENUM, GLOBAL_VAR, CONST_VAR, CONTRACT_DEFINITION -> Indent.getNoneIndent()
                TYPE_DEF -> Indent.getNormalIndent(true)
                UNION_TYPE_EXPRESSION -> Indent.getNoneIndent()
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
            ANNOTATION -> return Indent.getNoneIndent()
            FUNCTION -> return indentForFunctionChild(child)
            CONST_VAR -> return indentForConstantChild(child)
            GLOBAL_VAR -> return indentForGlobalVarChild(child)
            TYPE_DEF -> return indentForTypeDefChild(child)
            MATCH_EXPRESSION -> if (type != MATCH_KEYWORD) return indentIfNotBrace(child)
            CALL_EXPRESSION -> if (type == ARGUMENT_LIST) return Indent.getNormalIndent()
            PARAMETER_LIST, BLOCK_STATEMENT, STRUCT_EXPRESSION_BODY, STRUCT_BODY, ENUM_BODY, CONTRACT_BODY -> return indentIfNotBrace(child)
            DOT_EXPRESSION, TERNARY_EXPRESSION -> if (parent.firstChildNode != child) return Indent.getNormalIndent()
            VAR_TENSOR, TENSOR_EXPRESSION, PAREN_EXPRESSION, TENSOR_TYPE_EXPRESSION, PAREN_TYPE_EXPRESSION, ARGUMENT_LIST -> if (type != LPAREN && type != RPAREN) return Indent.getNormalIndent()
            VAR_TUPLE, TUPLE_TYPE_EXPRESSION, TUPLE_EXPRESSION -> if (type != LBRACK && type != RBRACK) return Indent.getNormalIndent()
            UNION_TYPE_EXPRESSION -> Indent.getContinuationIndent()
            FUNCTION_BODY -> if (type == BUILTIN_KEYWORD || type == ASM_DEFINITION) return Indent.getNormalIndent()
        }
        // todo: check if intent work normally
//        if (type == PRIMITIVE_TYPE_EXPRESSION || type == HOLE_TYPE_EXPRESSION) return Indent.getNoneIndent()
        return Indent.getNoneIndent()
    }

    private fun calcWrap(child: ASTNode): Wrap? {
        return null
    }

    companion object {
        private val BRACES_TOKEN_SET = TokenSet.create(
            LBRACE,
            RBRACE,
            LBRACK,
            RBRACK,
            LPAREN,
            RPAREN
        )

        private val FUNCTION_PARTS = TokenSet.create(
            FUNCTION_RECEIVER,
            IDENTIFIER,
            TYPE_PARAMETER_LIST,
            PARAMETER_LIST,
            RETURN_TYPE
        )

        private val CONST_PARTS = TokenSet.create(
            ANNOTATION,
            CONST_KEYWORD,
            IDENTIFIER
        )

        private val GLOBAL_VAR_PARTS = TokenSet.create(
            ANNOTATION,
            GLOBAL_KEYWORD,
            IDENTIFIER,
        )

        private val TYPE_DEF_PARTS = TokenSet.create(
            ANNOTATION,
            TYPE_KEYWORD,
            IDENTIFIER,
        )

        private fun indentIfNotBrace(child: ASTNode): Indent = when {
            BRACES_TOKEN_SET.contains(child.elementType) -> Indent.getNoneIndent()
            else -> Indent.getNormalIndent()
        }

        private fun indentForFunctionChild(child: ASTNode): Indent = when {
            FUNCTION_PARTS.contains(child.elementType) -> Indent.getNormalIndent()
            else -> Indent.getNoneIndent()
        }

        private fun indentForConstantChild(child: ASTNode): Indent = when {
            CONST_PARTS.contains(child.elementType) -> Indent.getNoneIndent()
            else -> Indent.getNormalIndent()
        }

        private fun indentForGlobalVarChild(child: ASTNode): Indent = when {
            GLOBAL_VAR_PARTS.contains(child.elementType) -> Indent.getNoneIndent()
            else -> Indent.getNormalIndent()
        }

        private fun indentForTypeDefChild(child: ASTNode): Indent = when {
            TYPE_DEF_PARTS.contains(child.elementType) -> Indent.getNoneIndent()
            else -> Indent.getNormalIndent()
        }
    }
}
