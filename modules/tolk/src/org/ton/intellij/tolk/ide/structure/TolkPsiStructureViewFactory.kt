package org.ton.intellij.tolk.ide.structure

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.TreeAnchorizer
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.containers.map2Array
import org.ton.intellij.tolk.psi.*

class TolkPsiStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        val tolkFile = psiFile as? TolkFile ?: return null
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return TolkStructureViewModel(tolkFile, editor)
            }
        }
    }
}

class TolkStructureViewModel(
    psiFile: TolkFile,
    editor: Editor?
) : StructureViewModelBase(psiFile, editor, TolkStructureViewElement(psiFile)), StructureViewModel.ElementInfoProvider {
    init {
        withSuitableClasses(
            TolkFunction::class.java,
            TolkGlobalVar::class.java,
            TolkConstVar::class.java,
            TolkTypeDef::class.java,
            TolkStruct::class.java,
            TolkStructField::class.java
        )
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean =
        element.value is TolkFile

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean {
        return when (element.value) {
            is TolkStructField,
            is TolkConstVar,
            is TolkGlobalVar,
            is TolkTypeDef -> true

            else -> false
        }
    }
}

class TolkStructureViewElement(
    psiElement: TolkElement
) : StructureViewTreeElement {
    private val psiAnchor = TreeAnchorizer.getService().createAnchor(psiElement)
    private val psi: TolkElement? get() = TreeAnchorizer.getService().retrieveElement(psiAnchor) as? TolkElement

    override fun navigate(requestFocus: Boolean) {
        (psi as? Navigatable)?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = (psi as? Navigatable)?.canNavigate() == true

    override fun canNavigateToSource(): Boolean = (psi as? Navigatable)?.canNavigateToSource() == true

    override fun getValue(): TolkElement? = psi

    override fun getPresentation(): ItemPresentation {
        val psi = psi ?: return PresentationData("", null, null, null)
        if (psi !is NavigatablePsiElement) return PresentationData("", null, null, null)
        val initialPresentation = psi.presentation ?: return PresentationData("", null, null, null)

        // override initialPresentation to avoid locationString and therefore the filename for each tree node
        return object : ItemPresentation {
            override fun getPresentableText() = initialPresentation.presentableText
            override fun getIcon(unused: Boolean) = initialPresentation.getIcon(unused)
        }
    }

    override fun getChildren(): Array<out TreeElement?> = childElements.map2Array { TolkStructureViewElement(it) }

    @Suppress("UNCHECKED_CAST")
    private val childElements: List<TolkElement>
        get() = when (val psi = psi) {
            is TolkFile -> psi.children.mapNotNull {
                when (it) {
                    is TolkFunction -> it
                    is TolkGlobalVar -> it
                    is TolkConstVar -> it
                    is TolkTypeDef -> it
                    is TolkStruct -> it
                    else -> null
                }
            }
            is TolkStruct -> psi.structBody?.structFieldList ?: emptyList()
            else -> emptyList()
        }
}
