package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkStructExpressionField
import org.ton.intellij.tolk.psi.TolkTypeArgumentList
import org.ton.intellij.tolk.psi.reference.TolkStructFieldReference

abstract class TolkStructExpressionFieldMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkStructExpressionField {
    override val typeArgumentList: TolkTypeArgumentList? get() = null

    override fun getReference() = TolkStructFieldReference(this)

    override val referenceNameElement: PsiElement? get() = identifier
}
