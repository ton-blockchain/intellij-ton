package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkMatchPatternReference
import org.ton.intellij.tolk.psi.reference.TolkMatchArmReference

abstract class TolkMatchPatternReferenceMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkMatchPatternReference {
    override fun getReference() = TolkMatchArmReference(this)
}
