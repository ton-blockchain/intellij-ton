package org.ton.intellij.tolk.ide.usages

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.TolkBinExpression
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.tolk.psi.TolkParenExpression
import org.ton.intellij.tolk.psi.TolkPrefixExpression
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkStructExpressionField
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.tolk.psi.TolkVar
import org.ton.intellij.tolk.psi.impl.functionSymbol
import org.ton.intellij.tolk.psi.impl.isMutable
import org.ton.intellij.tolk.psi.impl.isSetAssignment

class TolkReadWriteAccessDetector : ReadWriteAccessDetector() {
    override fun isReadWriteAccessible(element: PsiElement): Boolean {
        return element is TolkVar ||
                element is TolkConstVar ||
                element is TolkParameter ||
                element is TolkStructField
    }

    override fun isDeclarationWriteAccess(element: PsiElement): Boolean {
        return element is TolkVar || element is TolkConstVar
    }

    override fun getReferenceAccess(referencedElement: PsiElement, reference: PsiReference): Access {
        return getExpressionAccess(reference.element)
    }

    override fun getExpressionAccess(expression: PsiElement): Access {
        if (expression is TolkStructField) {
            val parent = expression.parent
            return if (parent is TolkStructExpressionField && parent.expression != null) Access.Write else Access.Read
        }
        val referenceExpression = expression as? TolkReferenceExpression ?: expression.parentOfType()
        return referenceExpression?.getReadWriteAccess() ?: Access.Read
    }

    fun TolkReferenceExpression.getReadWriteAccess(): Access {
        val expression = getConsiderableExpression(this)
        val parent = expression.parent
        if (parent is TolkBinExpression && (parent.binaryOp.eq != null || parent.isSetAssignment)) {
            if (!PsiTreeUtil.isAncestor(parent.left, expression, false)) return Access.Read
            // += or =
            return Access.Write
        }
        if (parent is TolkDotExpression) {
            val grand = parent.parent
            if (grand is TolkCallExpression) {
                val called = grand.functionSymbol ?: return Access.Read
                if (called.isMutable) {
                    return Access.Write
                }
                return Access.Read
            }
        }
        return Access.Read
    }

    private fun getConsiderableExpression(element: TolkExpression): TolkExpression {
        var result = element
        while (true) {
            val parent = result.parent ?: return result
            if (parent is TolkParenExpression) {
                result = parent
                continue
            }
            if (parent is TolkPrefixExpression) {
                result = parent
                continue
            }
            return result
        }
    }
}
