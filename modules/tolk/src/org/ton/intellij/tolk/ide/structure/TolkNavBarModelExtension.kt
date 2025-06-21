package org.ton.intellij.tolk.ide.structure

import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import com.intellij.util.Processor
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkSymbolElement

//class TolkNavBarModelExtension : StructureAwareNavBarModelExtension() {
//    override val language: Language get() = TolkLanguage
//
////    override fun createModel(file: PsiFile, editor: Editor?): StructureViewModel? {
////        if (file !is TolkFile) return null
////        return TolkStructureViewModel(file, editor)
////    }
//
//    override fun getPresentableText(item: Any?): String? {
//        println("nav=$item")

//    }
//
////    override fun getLeafElement(dataContext: DataContext): PsiElement? {
////        val leafElement = super.getLeafElement(dataContext) as? TolkElement ?: return null
////        if (TolkBreadcrumbsProvider().getElementInfo(leafElement).isEmpty()) return null
////        return leafElement
////    }
//
//    override fun getIcon(item: Any?): Icon? {
//        if (item is TolkElement) {
//            return item.getIcon(0)
//        }
//        return super.getIcon(item)
//    }
//}

class TolkNavBarModelExtension : StructureAwareNavBarModelExtension() {
    override val language: Language get() = TolkLanguage

    override fun getPresentableText(item: Any?): String? {
        val element = item as? TolkElement ?: return null
        if (element is TolkFile) {
            return element.name
        }
        if (element is TolkSymbolElement) {
            return element.name
        }
        return null
    }

    override fun getLeafElement(dataContext: DataContext): PsiElement? {
        val leaf =  super.getLeafElement(dataContext)
        return leaf
    }

    override fun processChildren(`object`: Any, rootElement: Any?, processor: Processor<Any>): Boolean {
        return super.processChildren(`object`, rootElement, processor)
    }

//    override fun adjustElement(psiElement: PsiElement): PsiElement? {
//        println("adjust=$psiElement")
//        if (psiElement is TolkFile || psiElement is PsiDirectory) return psiElement
//        if (psiElement is TolkFunction) return psiElement
//        if (psiElement is TolkSymbolElement && psiElement !is TolkLocalSymbolElement) {
//            return psiElement
//        }
//        return null
//    }
}
