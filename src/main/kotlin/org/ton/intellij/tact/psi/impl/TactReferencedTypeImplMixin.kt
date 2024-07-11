package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.ton.intellij.tact.psi.*
import org.ton.intellij.tact.resolve.TactTypeReference
import org.ton.intellij.tact.type.*

abstract class TactReferencedTypeImplMixin(
    node: ASTNode
) : TactTypeImpl(node), TactReferencedType {
    override fun getReference(): PsiReference? = TactTypeReference(this, identifier.textRangeInParent)
}

val TactType.ty: TactTy?
    get() = getType(this)

private fun getType(tactType: TactType): TactTy? {
    if (tactType is TactMapType) {
        val mapTypeItemList = tactType.mapTypeItemList
        val keyType = mapTypeItemList.getOrNull(0)?.referencedType?.ty ?: TactTyUnknown
        val valueType = mapTypeItemList.getOrNull(1)?.referencedType?.ty ?: TactTyUnknown
        return TactTyMap(keyType, valueType)
    }
    if (tactType is TactBouncedType) {
        return tactType.referencedType?.ty?.let { return TactTyBounced(it) }
    }
    val reference = tactType.reference ?: return null
    val typeDeclaration = reference.resolve() as? TactTypeDeclarationElement ?: return null
    val declaredTy = typeDeclaration.declaredTy
    if (tactType is TactReferencedType && tactType.q != null) return TactTyNullable(declaredTy)
    return declaredTy
}
