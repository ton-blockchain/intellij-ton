package org.ton.intellij.tolk.ide.structure

import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.refactoring.util.RefactoringDescriptionLocation
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.hasSelf

class TolkBreadcrumbsProvider : BreadcrumbsProvider {
    private interface TolkElementHandler<T : TolkElement> {
        fun accepts(e: PsiElement): Boolean
        fun elementInfo(e: T): String
    }

    private val handlers: List<TolkElementHandler<out TolkElement>> = listOf(
        handler({ it as? TolkFunction }) {
            val receiver = it.functionReceiver
            if (receiver != null) {
                append(receiver.text)
                append(".")
            }
            append(it.name)

            if (it.hasSelf) {
                append("(self)")
            } else {
                append("()")
            }
        },
        handler({ it as? TolkIfStatement }) {
            append("if ")
            val condition = it.condition
            if (condition != null) {
                append("(")
                append(condition.text.truncate(TextKind.INFO))
                append(")")
            }
        },
        handler({ it as? TolkElseBranch }) { append("else") },
        handler({ it as? TolkRepeatStatement }) { append("repeat") },
        handler({ it as? TolkDoStatement }) {
            append("do ")
            val condition = it.condition
            if (condition != null) {
                append("(")
                append(condition.text.truncate(TextKind.INFO))
                append(")")
            }
        },
        handler({ it as? TolkWhileStatement }) {
            append("while ")
            val condition = it.condition
            if (condition != null) {
                append("(")
                append(condition.text.truncate(TextKind.INFO))
                append(")")
            }
        },
        handler({
            (it as? TolkBlockStatement)?.takeIf { stmt -> stmt.parent is TolkTryStatement }
        }) {
            append("try")
        },
        handler({ it as? TolkCatch }) {
            append("catch ")
            val parameters = it.catchParameterList
            if (parameters.isNotEmpty()) {
                append("(")
                append(parameters.joinToString(", ") { param -> param.text.truncate(TextKind.INFO) })
                append(")")
            }
        },
        handler({ it as? TolkMatchExpression }) {
            append("match ")
            val expression = it.expression
            if (expression != null) {
                append("(")
                append(expression.text.truncate(TextKind.INFO))
                append(")")
            }
        },
        handler({ it as? TolkMatchArm }) {
            append(it.matchPattern.text.truncate(TextKind.INFO))
            append(" =>")
        },
        handler(
            {
                when (it) {
                    is TolkLocalSymbolElement -> null
                    is TolkSymbolElement      -> it
                    else                      -> null
                }
            }
        ) { append(it.name) },
    )

    private fun <T : TolkElement> handler(filter: (PsiElement) -> T?, string: StringBuilder.(T) -> Unit) =
        object : TolkElementHandler<T> {
            override fun accepts(e: PsiElement): Boolean = filter(e) != null
            override fun elementInfo(e: T): String = buildString {
                string(this, e)
            }
        }

    override fun getLanguages(): Array<TolkLanguage> = arrayOf(TolkLanguage)

    override fun acceptElement(element: PsiElement): Boolean = getHandler(element) != null

    override fun getElementInfo(element: PsiElement): @NlsSafe String = (element as? TolkElement)
        ?.let {
            getHandler(element)?.elementInfo(element)
        } ?: ""

    override fun getElementTooltip(element: PsiElement): @NlsSafe String {
        return ElementDescriptionUtil.getElementDescription(element, RefactoringDescriptionLocation.WITH_PARENT)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getHandler(e: PsiElement) =
        if (e is TolkElement) handlers.firstOrNull { it.accepts(e) } as? TolkElementHandler<TolkElement>
        else null

    companion object {
        @Suppress("unused")
        private enum class TextKind(val maxTextLength: Int) {
            INFO(16),
            TOOLTIP(100)
        }

        private const val ELLIPSIS = "${Typography.ellipsis}"

        private fun String.truncate(kind: TextKind): String {
            val maxLength = kind.maxTextLength
            return if (length > maxLength)
                "${substring(0, maxLength - ELLIPSIS.length)}$ELLIPSIS"
            else this
        }
    }
}
