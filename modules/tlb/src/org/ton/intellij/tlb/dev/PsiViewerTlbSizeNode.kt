package org.ton.intellij.tlb.dev

import com.intellij.dev.psiViewer.properties.tree.PsiViewerPropertyNode
import com.intellij.dev.psiViewer.properties.tree.nodes.computePsiViewerApiClassesNodes
import com.intellij.dev.psiViewer.properties.tree.nodes.psiViewerApiClassesExtending
import com.intellij.dev.psiViewer.properties.tree.nodes.psiViewerPsiTypeAttributes
import org.ton.intellij.tlb.TlbSize

class PsiViewerTlbSizeNode(
    private val size: TlbSize,
    private val nodeContext: PsiViewerPropertyNode.Context
) : PsiViewerPropertyNode {
    class Factory : PsiViewerPropertyNode.Factory {
        override suspend fun createNode(
            nodeContext: PsiViewerPropertyNode.Context,
            returnedValue: Any
        ): PsiViewerPropertyNode? {
            val type = returnedValue as? TlbSize ?: return null
            return PsiViewerTlbSizeNode(type, nodeContext)
        }

        override fun isMatchingType(clazz: Class<*>): Boolean = TlbSize::class.java.isAssignableFrom(clazz)
    }

    override val children = PsiViewerPropertyNode.Children.Async {
        val psiTypeApiClasses = size::class.java.psiViewerApiClassesExtending(TlbSize::class.java)
        computePsiViewerApiClassesNodes(psiTypeApiClasses, size, nodeContext)
    }

    override val presentation = PsiViewerPropertyNode.Presentation {
        it.append(size.toString(), psiViewerPsiTypeAttributes())
    }

    override val weight: Int = 25
}