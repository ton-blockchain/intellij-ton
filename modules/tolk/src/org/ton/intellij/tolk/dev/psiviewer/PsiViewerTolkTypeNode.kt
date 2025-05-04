package org.ton.intellij.tolk.dev.psiviewer

import com.intellij.dev.psiViewer.properties.tree.PsiViewerPropertyNode
import com.intellij.dev.psiViewer.properties.tree.nodes.computePsiViewerApiClassesNodes
import com.intellij.dev.psiViewer.properties.tree.nodes.psiViewerApiClassesExtending
import com.intellij.dev.psiViewer.properties.tree.nodes.psiViewerPsiTypeAttributes
import org.ton.intellij.tolk.type.TolkTy

class PsiViewerTolkTypeNode(
    private val type: TolkTy,
    private val nodeContext: PsiViewerPropertyNode.Context
) : PsiViewerPropertyNode {
    class Factory : PsiViewerPropertyNode.Factory {
        override suspend fun createNode(
            nodeContext: PsiViewerPropertyNode.Context,
            returnedValue: Any
        ): PsiViewerPropertyNode? {
            val type = returnedValue as? TolkTy ?: return null
            return PsiViewerTolkTypeNode(type, nodeContext)
        }

        override fun isMatchingType(clazz: Class<*>): Boolean = TolkTy::class.java.isAssignableFrom(clazz)
    }

    override val children = PsiViewerPropertyNode.Children.Async {
        val psiTypeApiClasses = type::class.java.psiViewerApiClassesExtending(TolkTy::class.java)
        computePsiViewerApiClassesNodes(psiTypeApiClasses, type, nodeContext)
    }

    override val presentation = PsiViewerPropertyNode.Presentation {
        it.append(type.toString(), psiViewerPsiTypeAttributes())
    }

    override val weight: Int = 25
}
