package org.ton.intellij.fift.ide.structure

import com.intellij.ide.structureView.*
import com.intellij.ide.structureView.StructureViewModel.ElementInfoProvider
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.ide.util.treeView.smartTree.Filter
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.fift.FiftIcons
import org.ton.intellij.fift.psi.FiftDefinition
import org.ton.intellij.fift.psi.FiftFile
import org.ton.intellij.fift.psi.FiftNamedElement
import org.ton.intellij.fift.psi.name
import javax.swing.Icon

class FiftStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel = Model(psiFile, editor)
            override fun isRootNodeShown() = false
        }
    }

    class Model(file: PsiFile, editor: Editor?) : StructureViewModelBase(file, editor, Element(file)), ElementInfoProvider {
        init {
            withSuitableClasses(FiftFile::class.java, FiftNamedElement::class.java)
        }

        override fun getFilters() = arrayOf<Filter>()
        override fun isAlwaysShowsPlus(element: StructureViewTreeElement) = false
        override fun isAlwaysLeaf(element: StructureViewTreeElement) = false
    }

    class Element(element: PsiElement) : PsiTreeElementBase<PsiElement>(element) {
        override fun getChildrenBase(): MutableCollection<StructureViewTreeElement> {
            val result = mutableListOf<StructureViewTreeElement>()
            val element = element

            if (element is FiftFile) {
                element.assemblyDefinitions().forEach { result.add(Element(it)) }
            }

            return result
        }

        override fun getPresentableText(): String? {
            val element = element

            if (element is FiftFile) {
                return element.name
            }

            if (element is FiftDefinition) {
                return element.name()
            }

            return null
        }

        override fun getIcon(open: Boolean): Icon? {
            val element = element

            if (element is FiftDefinition) {
                if (element.methodDefinition != null) {
                    return FiftIcons.METHOD
                }
                if (element.procDefinition != null) {
                    return FiftIcons.FUNCTION
                }
                if (element.procInlineDefinition != null) {
                    return FiftIcons.FUNCTION
                }
                if (element.procRefDefinition != null) {
                    return FiftIcons.FUNCTION
                }
            }

            return null
        }
    }
}
