package org.ton.intellij.tact.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import org.ton.intellij.tact.psi.TactConstant
import org.ton.intellij.tact.psi.TactElement
import org.ton.intellij.tact.psi.TactFile
import org.ton.intellij.tact.psi.TactReferenceExpression
import org.ton.intellij.tact.resolve.TactFieldReference
import org.ton.intellij.util.parentOfType

abstract class TactReferenceExpressionImplMixin(
    node: ASTNode
) : ASTWrapperPsiElement(node), TactReferenceExpression {
    override fun getReference(): PsiReference? {
        if (name == "_") {
            return null
        }
        return TactReferenceExpressionReference(this, identifier.textRangeInParent)
    }
}

class TactReferenceExpressionReference(
    element: TactReferenceExpression,
    textRange: TextRange
) : TactFieldReference<TactReferenceExpression>(element, textRange) {
    override fun multiResolve(): Collection<TactElement> {
        return super.multiResolve().ifEmpty {
            val parentConstant = element.parentOfType<TactConstant>() ?: return@ifEmpty emptyList()
            val file = parentConstant.parent as? TactFile ?: return@ifEmpty emptyList()
            for (constant in file.constants) {
                if (constant == parentConstant) {
                    break
                }
                if (constant.name == element.identifier.text) {
                    return listOf(constant)
                }
            }
            emptyList()
        }
    }
}
