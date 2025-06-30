package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkMatchPatternReference
import org.ton.intellij.tolk.psi.TolkTypeArgumentList
import org.ton.intellij.tolk.psi.reference.TolkMatchArmReference

abstract class TolkMatchPatternReferenceMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkMatchPatternReference {
    override val typeArgumentList: TolkTypeArgumentList?
        get() = findChildByClass(TolkTypeArgumentList::class.java)

    override val referenceNameElement: PsiElement? get() = identifier

    override fun getReference() = TolkMatchArmReference(this)
}
