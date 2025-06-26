package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.TolkFunctionReceiver
import org.ton.intellij.tolk.psi.TolkReferenceTypeExpression
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkTypedElement
import org.ton.intellij.tolk.psi.reference.TolkTypeReference
import org.ton.intellij.tolk.type.TolkStructTy
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyParam

abstract class TolkReferenceTypeExpressionMixin : ASTWrapperPsiElement, TolkReferenceTypeExpression {

    constructor(node: ASTNode) : super(node)
//    constructor(stub: TolkReferenceTypeExpressionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    private val primitiveType: TolkTy?
        get() {
            return TolkTy.byName(referenceName ?: return null)
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
        val primitiveType = TolkTy.byName(referenceName)
        if (primitiveType != null) {
            return null
        }
        return TolkTypeReference(this)
    }

    private fun resolveType(): TolkTy? {
        val primitiveType = primitiveType
        if (primitiveType != null) return primitiveType

        val resolved = reference?.resolve()
        return when {
            resolved is TolkStruct -> TolkStructTy.create(resolved, typeArgumentList?.typeExpressionList)

            resolved is TolkTypedElement -> resolved.type

            parentOfType<TolkFunctionReceiver>() != null && typeArgumentList == null -> TolkTyParam.create(this)

            else -> TolkTy.Unknown
        }
    }


//    inner class CacheResolver : CachedValueProvider<TolkTy> {
//        override fun compute(): CachedValueProvider.Result<TolkTy?>? {
//            resolve().firstOrNull()?.let { return CachedValueProvider.Result.create(it, this) }
//            return CachedValueProvider.Result.create(null, this)
//        }
//
//        fun resolve(): List<TolkTy> = buildList {
//            val typeName = text
//            when(typeName) {
//                "int" -> return listOf(TolkTyInt)
//                "cell" -> return listOf(TolkTyCell)
//                "slice" -> return listOf(TolkTySlice)
//                "builder" -> return listOf(TolkTyBuilder)
//                "continuation" -> return listOf(TolkTyCont)
//                "tuple" -> return listOf(TolkTyAtomicTuple)
//            }
//            val owner = parentOfType<TolkTypeParameterListOwner>()
//            if (owner != null) {
//                val typeParameterList = owner.typeParameterList
//                typeParameterList?.typeParameterList?.forEach { typeParameter ->
//                    if (typeParameter.name == typeName) {
//                        add(TolkTyParameter(typeParameter))
//                    }
//                }
//            }
//            TolkTypeDeclarationIndex.findElementsByName(
//                project,
//                typeName,
//                GlobalSearchScope.filesScope(project) {
//                    val file = containingFile as? TolkFile ?: return@filesScope emptyList()
//                    file.collectIncludedFiles(true).map { it.virtualFile }
//                }
//            ).forEach { typeDeclaration ->
//                add(TolkTyTypeDeclaration(typeDeclaration))
//            }
//        }
//    }
}

val TolkReferenceTypeExpression.isPrimitive: Boolean
    get() = (this as TolkReferenceTypeExpressionMixin).isPrimitive
