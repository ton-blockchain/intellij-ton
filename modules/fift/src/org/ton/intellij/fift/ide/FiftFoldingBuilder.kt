package org.ton.intellij.fift.ide

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import org.ton.intellij.fift.psi.FiftTypes
import com.intellij.openapi.util.TextRange

class FiftFoldingBuilder : FoldingBuilder {
    override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
        return buildList {
            collectDescriptorsRecursively(node, this)
        }.toTypedArray()
    }

    private fun collectDescriptorsRecursively(node: ASTNode, descriptors: MutableList<FoldingDescriptor>) {
        when (node.elementType) {
            FiftTypes.PROC_INLINE_DEFINITION,
            FiftTypes.PROC_DEFINITION,
            FiftTypes.PROC_REF_DEFINITION,
            FiftTypes.METHOD_DEFINITION,
                 -> {
                val psi = node.psi
                val start = psi.firstChild.nextSibling.nextSibling
                val end = psi.lastChild
                descriptors.add(FoldingDescriptor(node, TextRange(start.endOffset, end.startOffset)))
            }
        }

        node.getChildren(null).forEach { childNode ->
            collectDescriptorsRecursively(childNode, descriptors)
        }
    }

    override fun getPlaceholderText(node: ASTNode): String = when (node.elementType) {
        FiftTypes.DEFINITION,
             -> "{...}"

        else -> "..."
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
