package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.reference.TolkTypeReference
import org.ton.intellij.tolk.stub.TolkTypeStub
import org.ton.intellij.tolk.type.TolkTyStruct
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyAlias
import org.ton.intellij.tolk.type.TolkTyParam

abstract class TolkReferenceTypeExpressionMixin : TolkTypeExpressionImpl, TolkReferenceTypeExpression {
    constructor(node: ASTNode) : super(node)
    constructor(stub: TolkTypeStub<*>, stubType: IStubElementType<*, *>) : super(stub, stubType)

    private val primitiveType: TolkTy?
        get() {
            return TolkTy.byName(referenceName ?: return null)
        }

    override val referenceName: String?
        get() {
            val stub = stub
            if (stub != null) {
                val text = stub.getText()
                if (text != null) {
                    if (text.contains("<")) {
                        // Foo<T> -> Foo
                        return text.substringBefore("<")
                    }
                    return text
                }
            }
            return super.referenceName
        }

    val isPrimitive get() = primitiveType != null

    override val type: TolkTy?
        get() = CachedValuesManager.getCachedValue(this) {
            val type = resolveType()
            CachedValueProvider.Result.create(type, this)
        }

    override val referenceNameElement: PsiElement get() = identifier

    override fun getReference(): TolkTypeReference? {
        val referenceName = referenceName ?: return null
        val reference = TolkTypeReference(this)
        if (reference.resolve() != null) {
            return reference
        }
        val primitiveType = TolkTy.byName(referenceName)
        if (primitiveType != null) {
            return null
        }
        return reference
    }

    private fun resolveType(): TolkTy? {
        val primitiveType = primitiveType
        val resolved = reference?.resolve()
        return when {
            resolved is TolkStruct -> TolkTyStruct.create(resolved, typeArgumentList?.typeExpressionList)
            resolved is TolkTypeDef && primitiveType == null -> TolkTyAlias.create(resolved, typeArgumentList?.typeExpressionList)
            resolved is TolkTypeParameter -> resolved.type
            resolved is TolkTypedElement && primitiveType == null -> resolved.type
            parentOfType<TolkFunctionReceiver>() != null && typeArgumentList == null -> {
                primitiveType ?: TolkTyParam.create(this)
            }
            else -> primitiveType
        }
    }
}
