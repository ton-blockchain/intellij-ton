package org.ton.intellij.tlb.structure

import com.intellij.ide.structureView.*
import com.intellij.ide.structureView.StructureViewModel.ElementInfoProvider
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.ide.util.treeView.smartTree.Filter
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.TlbFile
import org.ton.intellij.tlb.psi.TlbNamedElement

class TlbStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel = Model(psiFile, editor)
            override fun isRootNodeShown() = false
        }
    }

    class Model(file: PsiFile, editor: Editor?) : StructureViewModelBase(file, editor, Element(file)), ElementInfoProvider {
        init {
            withSuitableClasses(TlbFile::class.java, TlbNamedElement::class.java)
        }

        override fun getFilters() = arrayOf<Filter>()
        override fun isAlwaysShowsPlus(element: StructureViewTreeElement) = false
        override fun isAlwaysLeaf(element: StructureViewTreeElement) = false
    }

    class Element(element: PsiElement) : PsiTreeElementBase<PsiElement>(element) {
        override fun getChildrenBase(): MutableCollection<StructureViewTreeElement> {
            val result = mutableListOf<StructureViewTreeElement>()
            val element = element

            if (element is TlbFile) {
                element.constructors().forEach { result.add(Element(it)) }
            }

            return result
        }

        override fun getPresentableText(): String? {
            val element = element

            if (element is TlbFile) {
                return element.name
            }

            if (element is TlbConstructor) {
                val resultType = element.resultType ?: return null
                return resultType.text + " = " + element.text
            }

            return null
        }
    }
}
