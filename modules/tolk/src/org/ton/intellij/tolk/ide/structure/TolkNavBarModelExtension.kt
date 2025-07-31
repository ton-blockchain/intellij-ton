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
}
