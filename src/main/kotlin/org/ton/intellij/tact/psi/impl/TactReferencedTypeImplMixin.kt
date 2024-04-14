package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.ton.intellij.tact.psi.TactReferencedType
import org.ton.intellij.tact.psi.TactType
import org.ton.intellij.tact.psi.TactTypeDeclarationElement
import org.ton.intellij.tact.resolve.TactTypeReference
import org.ton.intellij.tact.type.TactTy
import org.ton.intellij.tact.type.TactTyNullable

abstract class TactReferencedTypeImplMixin(
    node: ASTNode
) : TactTypeImpl(node), TactReferencedType {
    override fun getReference(): PsiReference? = TactTypeReference(this, identifier.textRangeInParent)
}

val TactType.ty: TactTy?
    get() = (reference?.resolve() as? TactTypeDeclarationElement)?.declaredType?.let {
        if (this is TactReferencedType && q != null) TactTyNullable(it) else it
    }
