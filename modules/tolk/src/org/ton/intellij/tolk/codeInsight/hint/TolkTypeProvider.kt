package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.lang.ExpressionTypeProvider
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parents
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.render

class TolkTypeProvider : ExpressionTypeProvider<TolkTypedElement>() {
    override fun getInformationHint(elementAt: TolkTypedElement): @NlsContexts.HintText String {
        return StringUtil.escapeXmlEntities(typePresentation(elementAt))
    }

    private fun typePresentation(element: TolkTypedElement): String {
        return (element.type ?: TolkTy.Unknown).render()
    }

    override fun getErrorHint(): @NlsContexts.HintText String = TolkBundle.message("codeInsight.hint.error.hint")

    override fun getExpressionsAt(elementAt: PsiElement): List<TolkTypedElement> {
        return elementAt.parents(true).filterIsInstance<TolkExpression>().filter {
            // remove reference to function (with type `(..)->(..)`) in call expression
            it !is TolkReferenceExpression || it.node.treeParent.elementType != TolkElementTypes.CALL_EXPRESSION
        }.filter {
            // remove `0` from `t.0`
            it !is TolkLiteralExpression || it.node.treeParent.elementType != TolkElementTypes.DOT_EXPRESSION
        }.filter {
            val parent = it.parent
            // remove `bar()` from `foo.bar()`
            it !is TolkCallExpression || parent !is TolkDotExpression || (parent.fieldLookup != it)
        }
            .filter { // remove __expect_type call result
                val callExpr = it as? TolkCallExpression ?: return@filter true
                val refExpr = callExpr.expression as? TolkReferenceExpression ?: return@filter true
                refExpr.referenceName != "__expect_type"
            }

            .toList()
    }
}
