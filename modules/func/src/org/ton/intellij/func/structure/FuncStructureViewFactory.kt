package org.ton.intellij.func.structure

import com.intellij.ide.structureView.*
import com.intellij.ide.structureView.StructureViewModel.ElementInfoProvider
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.ide.util.treeView.smartTree.Filter
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.func.FuncIcons
import org.ton.intellij.func.psi.FuncConstVar
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncGlobalVar
import org.ton.intellij.func.psi.FuncIncludeDefinition
import org.ton.intellij.func.psi.FuncNamedElement
import javax.swing.Icon

class FuncStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel = Model(psiFile, editor)
            override fun isRootNodeShown() = false
        }
    }

    class Model(file: PsiFile, editor: Editor?) : StructureViewModelBase(file, editor, Element(file)), ElementInfoProvider {
        init {
            withSuitableClasses(FuncFile::class.java, FuncNamedElement::class.java)
        }

        override fun getFilters() = arrayOf<Filter>()
        override fun isAlwaysShowsPlus(element: StructureViewTreeElement) = false
        override fun isAlwaysLeaf(element: StructureViewTreeElement) = false
    }

    class Element(element: PsiElement) : PsiTreeElementBase<PsiElement>(element) {
        override fun getChildrenBase(): MutableCollection<StructureViewTreeElement> {
            val result = mutableListOf<StructureViewTreeElement>()
            val element = element

            if (element is FuncFile) {
                element.includeDefinitions.forEach { result.add(Element(it)) }
                element.globalVars.forEach { result.add(Element(it)) }
                element.constVars.forEach { result.add(Element(it)) }
                element.functions.forEach { result.add(Element(it)) }
            }

            return result
        }

        override fun getPresentableText(): String? {
            val element = element

            if (element is FuncFile) {
                return element.name
            }

            if (element is FuncIncludeDefinition) {
                return element.text
            }

            if (element is FuncFunction) {
                val name = element.name
                val parameterList = element.functionParameterList
                val returnType = element.typeReference.text
                val signature = parameterList.joinToString(", ", prefix = "(", postfix = ")") { it.text } + " -> " + returnType
                return name + signature
            }

            if (element is FuncConstVar) {
                val expression = element.expression?.text ?: ""
                val type = element.sliceKeyword?.text ?: element.intKeyword?.text
                if (type == null) return "${element.name} = $expression"

                return "${element.name}: $type = $expression"
            }

            if (element is FuncGlobalVar) {
                val type = element.typeReference.text
                return "${element.name}: $type"
            }

            if (element is FuncNamedElement) {
                return element.name
            }

            return null
        }

        override fun getIcon(open: Boolean): Icon? {
            if (element is FuncIncludeDefinition) {
                return FuncIcons.FILE
            }

            return super.getIcon(open)
        }
    }
}
