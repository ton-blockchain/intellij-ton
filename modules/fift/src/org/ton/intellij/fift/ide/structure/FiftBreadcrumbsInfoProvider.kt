package org.ton.intellij.fift.ide.structure

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import org.ton.intellij.fift.FiftLanguage
import org.ton.intellij.fift.psi.FiftDefinition
import org.ton.intellij.fift.psi.FiftElement
import org.ton.intellij.fift.psi.FiftIfStatement
import org.ton.intellij.fift.psi.FiftIfjmpStatement
import org.ton.intellij.fift.psi.FiftUntilStatement
import org.ton.intellij.fift.psi.FiftWhileStatement
import org.ton.intellij.fift.psi.name

class FiftBreadcrumbsInfoProvider : BreadcrumbsProvider {
    private interface ElementHandler<T : FiftElement> {
        fun accepts(e: PsiElement): Boolean
        fun elementInfo(e: T): String
    }

    private val handlers = listOf<ElementHandler<*>>(
        FuncFunctionHandler,
        FuncIfHandler,
        FuncIfJmpHandler,
        FuncWhileHandler,
        FuncUntilHandler,
    )

    private object FuncFunctionHandler : ElementHandler<FiftDefinition> {
        override fun accepts(e: PsiElement): Boolean = e is FiftDefinition

        override fun elementInfo(e: FiftDefinition): String = e.name() ?: ""
    }

    private object FuncIfHandler : ElementHandler<FiftIfStatement> {
        override fun accepts(e: PsiElement): Boolean = e is FiftIfStatement

        override fun elementInfo(e: FiftIfStatement): String = buildString {
            append("IF ")
        }
    }

    private object FuncIfJmpHandler : ElementHandler<FiftIfjmpStatement> {
        override fun accepts(e: PsiElement): Boolean = e is FiftIfjmpStatement

        override fun elementInfo(e: FiftIfjmpStatement): String = buildString {
            append("IFJMP ")
        }
    }

    private object FuncWhileHandler : ElementHandler<FiftWhileStatement> {
        override fun accepts(e: PsiElement): Boolean = e is FiftWhileStatement

        override fun elementInfo(e: FiftWhileStatement): String = buildString {
            append("WHILE ")
        }
    }

    private object FuncUntilHandler : ElementHandler<FiftUntilStatement> {
        override fun accepts(e: PsiElement): Boolean = e is FiftUntilStatement

        override fun elementInfo(e: FiftUntilStatement): String = buildString {
            append("UNTIL ")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handler(e: PsiElement): ElementHandler<in FiftElement>? {
        return if (e is FiftElement)
            handlers.firstOrNull { it.accepts(e) } as ElementHandler<in FiftElement>?
        else null
    }

    override fun getLanguages(): Array<Language> = arrayOf(FiftLanguage)
    override fun acceptElement(e: PsiElement): Boolean = handler(e) != null
    override fun getElementInfo(e: PsiElement): String = handler(e)!!.elementInfo(e as FiftElement)
}
