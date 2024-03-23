package org.ton.intellij.tact

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.ton.intellij.tact.psi.TactBlock
import org.ton.intellij.tact.psi.TactContractBody
import org.ton.intellij.tact.psi.TactElementTypes
import org.ton.intellij.tact.psi.TactRecursiveElementWalkingVisitor

class TactFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        root.accept(object : TactRecursiveElementWalkingVisitor() {
            override fun visitBlock(o: TactBlock) {
                super.visitBlock(o)
                descriptors.add(FoldingDescriptor(o.node, o.textRange))
            }

            override fun visitContractBody(o: TactContractBody) {
                super.visitContractBody(o)
                descriptors.add(FoldingDescriptor(o.node, o.textRange))
            }
        })
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(p0: ASTNode): String? {
        return when (p0.elementType) {
            TactElementTypes.BLOCK,
            TactElementTypes.CONTRACT_BODY -> "{...}"

            else -> null
        }
    }

    override fun isCollapsedByDefault(p0: ASTNode): Boolean = false

}
