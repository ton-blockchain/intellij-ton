package org.ton.intellij.func.ide

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import org.ton.intellij.func.psi.FuncElementTypes

class FuncFoldingBuilder : FoldingBuilder {
    override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
        return buildList {
            collectDescriptorsRecursively(node, this)
        }.toTypedArray()
    }

    private fun collectDescriptorsRecursively(node: ASTNode, descriptors: MutableList<FoldingDescriptor>) {
        when (node.elementType) {
            FuncElementTypes.BLOCK_STATEMENT,
                -> descriptors.add(FoldingDescriptor(node, node.textRange))
        }

        node.getChildren(null).forEach { childNode ->
            collectDescriptorsRecursively(childNode, descriptors)
        }
    }

    override fun getPlaceholderText(node: ASTNode): String = when (node.elementType) {
        FuncElementTypes.BLOCK_STATEMENT,
            -> "{...}"

        else -> "..."
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
