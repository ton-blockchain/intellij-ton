package org.ton.intellij.func.template

import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.FuncBlockStatement
import org.ton.intellij.func.psi.FuncExpressionStatement

class FuncStatementContextType : FuncTemplateContextType("FunC (statement)") {
    override fun isInContext(o: PsiElement): Boolean {
        if (o.parent?.parent is FuncExpressionStatement && o.parent?.parent?.parent is FuncBlockStatement) {
            return true
        }

        val grand = o.parent?.parent?.parent?.parent
        return grand is FuncExpressionStatement && grand.parent is FuncBlockStatement
    }
}
