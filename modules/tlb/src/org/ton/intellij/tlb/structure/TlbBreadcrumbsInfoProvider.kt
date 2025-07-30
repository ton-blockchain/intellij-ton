package org.ton.intellij.tlb.structure

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import org.ton.intellij.tlb.TlbLanguage
import org.ton.intellij.tlb.psi.TlbCommonField
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.TlbElement

class TlbBreadcrumbsInfoProvider : BreadcrumbsProvider {
    private interface ElementHandler<T : TlbElement> {
        fun accepts(e: PsiElement): Boolean
        fun elementInfo(e: T): String
    }

    private val handlers = listOf<ElementHandler<*>>(
        TlbConstructorHandler,
        TlbFieldHandler,
    )

    private object TlbConstructorHandler : ElementHandler<TlbConstructor> {
        override fun accepts(e: PsiElement): Boolean = e is TlbConstructor

        override fun elementInfo(e: TlbConstructor): String = e.resultType?.name ?: e.name ?: ""
    }

    private object TlbFieldHandler : ElementHandler<TlbCommonField> {
        override fun accepts(e: PsiElement): Boolean = e is TlbCommonField

        override fun elementInfo(e: TlbCommonField): String = e.name ?: e.text
    }

    @Suppress("UNCHECKED_CAST")
    private fun handler(e: PsiElement): ElementHandler<in TlbElement>? {
        return if (e is TlbElement)
            handlers.firstOrNull { it.accepts(e) } as ElementHandler<in TlbElement>?
        else null
    }

    override fun getLanguages(): Array<Language> = arrayOf(TlbLanguage)
    override fun acceptElement(e: PsiElement): Boolean = handler(e) != null
    override fun getElementInfo(e: PsiElement): String = handler(e)!!.elementInfo(e as TlbElement)
}
