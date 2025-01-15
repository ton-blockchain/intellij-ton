package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.lang.ExpressionTypeProvider
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parents
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.psi.*

class TolkTypeProvider : ExpressionTypeProvider<TolkTypedElement>() {
    override fun getInformationHint(elementAt: TolkTypedElement): @NlsContexts.HintText String {
        return StringUtil.escapeXmlEntities(typePresentation(elementAt))
    }

    private fun typePresentation(element: TolkTypedElement): String {
        return element.type?.toString() ?: "<unknown>"
    }

    override fun getErrorHint(): @NlsContexts.HintText String = TolkBundle.message("codeInsight.hint.error_hint")

    override fun getExpressionsAt(elementAt: PsiElement): List<TolkTypedElement> {
        return elementAt.parents(true).filterIsInstance<TolkExpression>().filter {
            // remove reference to function (with type `(..)->(..)`) in call expression
            it !is TolkReferenceExpression || it.parent !is TolkCallExpression
        }.filter {
            val parent = it.parent
            // remove `bar()` from `foo.bar()`
            it !is TolkCallExpression || parent !is TolkDotExpression || parent.right != it
        }.toList()
    }
}