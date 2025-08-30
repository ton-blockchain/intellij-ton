package org.ton.intellij.func.template

import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.FuncReferenceExpression

class FuncExpressionContextType : FuncTemplateContextType("FunC (expression)") {
    override fun isInContext(o: PsiElement): Boolean {
        return o.parent is FuncReferenceExpression
    }
}
