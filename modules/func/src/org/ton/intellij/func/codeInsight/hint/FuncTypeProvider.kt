package org.ton.intellij.func.codeInsight.hint

import com.intellij.lang.ExpressionTypeProvider
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parents
import org.ton.intellij.func.FuncBundle
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncExpression
import org.ton.intellij.func.psi.FuncReferenceExpression
import org.ton.intellij.func.psi.inference

class FuncTypeProvider : ExpressionTypeProvider<FuncExpression>() {
    override fun getInformationHint(elementAt: FuncExpression): @NlsContexts.HintText String {
        return StringUtil.escapeXmlEntities(typePresentation(elementAt))
    }

    private fun typePresentation(element: FuncExpression): String {
        val inference = element.inference ?: return "<unknown>"
        return (inference.getExprTy(element)).toString()
    }

    override fun getErrorHint(): @NlsContexts.HintText String = FuncBundle.message("codeInsight.hint.error.hint")

    override fun getExpressionsAt(elementAt: PsiElement): List<FuncExpression> {
        return elementAt.parents(true).filterIsInstance<FuncExpression>().filter {
            // remove reference to function (with type `(..)->(..)`) in call expression
            it !is FuncReferenceExpression || it.node.treeParent.elementType != FuncElementTypes.SPECIAL_APPLY_EXPRESSION
        }
            .toList()
    }
}

