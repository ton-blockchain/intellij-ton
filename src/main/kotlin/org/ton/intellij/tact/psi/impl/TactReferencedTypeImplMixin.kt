package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.ton.intellij.tact.psi.TactReferencedType
import org.ton.intellij.tact.resolve.TactTypeReference

abstract class TactReferencedTypeImplMixin(
    node: ASTNode
) : TactTypeImpl(node), TactReferencedType {
    override fun getReference(): PsiReference? = TactTypeReference(this, identifier.textRangeInParent)
}
