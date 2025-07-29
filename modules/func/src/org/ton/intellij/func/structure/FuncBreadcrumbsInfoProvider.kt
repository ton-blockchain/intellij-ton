package org.ton.intellij.func.structure

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.psi.FuncCatch
import org.ton.intellij.func.psi.FuncDoStatement
import org.ton.intellij.func.psi.FuncElement
import org.ton.intellij.func.psi.FuncElseBranch
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncIfStatement
import org.ton.intellij.func.psi.FuncTryStatement
import org.ton.intellij.func.psi.FuncWhileStatement

class FuncBreadcrumbsInfoProvider : BreadcrumbsProvider {
    private interface ElementHandler<T : FuncElement> {
        fun accepts(e: PsiElement): Boolean
        fun elementInfo(e: T): String
    }

    private val handlers = listOf<ElementHandler<*>>(
        FuncFunctionHandler,
        FuncIfHandler,
        FuncElseHandler,
        FuncWhileHandler,
        FuncDoHandler,
        FuncTryHandler,
        FuncCatchHandler,
    )

    private object FuncFunctionHandler : ElementHandler<FuncFunction> {
        override fun accepts(e: PsiElement): Boolean = e is FuncFunction

        override fun elementInfo(e: FuncFunction): String = "${e.name}()"
    }

    private object FuncIfHandler : ElementHandler<FuncIfStatement> {
        override fun accepts(e: PsiElement): Boolean = e is FuncIfStatement

        override fun elementInfo(e: FuncIfStatement): String = buildString {
            append("if ")
            val condition = e.expression
            if (condition != null) {
                append(condition.text.truncate())
            } else {
                append("?")
            }
        }
    }

    private object FuncElseHandler : ElementHandler<FuncElseBranch> {
        override fun accepts(e: PsiElement): Boolean = e is FuncElseBranch

        override fun elementInfo(e: FuncElseBranch): String = buildString {
            append("else")
        }
    }

    private object FuncWhileHandler : ElementHandler<FuncWhileStatement> {
        override fun accepts(e: PsiElement): Boolean = e is FuncWhileStatement

        override fun elementInfo(e: FuncWhileStatement): String = buildString {
            append("while ")
            val condition = e.expression
            if (condition != null) {
                append(condition.text.truncate())
            } else {
                append("?")
            }
        }
    }

    private object FuncDoHandler : ElementHandler<FuncDoStatement> {
        override fun accepts(e: PsiElement): Boolean = e is FuncDoStatement

        override fun elementInfo(e: FuncDoStatement): String = buildString {
            append("do {} until ")
            val condition = e.expression
            if (condition != null) {
                append(condition.text.truncate())
            } else {
                append("?")
            }
        }
    }

    private object FuncTryHandler : ElementHandler<FuncTryStatement> {
        override fun accepts(e: PsiElement): Boolean = e is FuncTryStatement

        override fun elementInfo(e: FuncTryStatement): String = buildString {
            append("try ")
        }
    }

    private object FuncCatchHandler : ElementHandler<FuncCatch> {
        override fun accepts(e: PsiElement): Boolean = e is FuncCatch

        override fun elementInfo(e: FuncCatch): String = buildString {
            append("catch ")
            val variables = e.expression
            if (variables != null) {
                append(variables.text.truncate())
            } else {
                append("?")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handler(e: PsiElement): ElementHandler<in FuncElement>? {
        return if (e is FuncElement)
            handlers.firstOrNull { it.accepts(e) } as ElementHandler<in FuncElement>?
        else null
    }

    override fun getLanguages(): Array<Language> = arrayOf(FuncLanguage)
    override fun acceptElement(e: PsiElement): Boolean = handler(e) != null
    override fun getElementInfo(e: PsiElement): String = handler(e)!!.elementInfo(e as FuncElement)
}

private const val ELLIPSIS = "${Typography.ellipsis}"

private fun String.truncate(): String {
    return if (length > 16) "${substring(0, 16 - ELLIPSIS.length)}$ELLIPSIS"
    else this
}
