package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.TolkFunctionReceiver
import org.ton.intellij.tolk.psi.TolkReferenceTypeExpression
import org.ton.intellij.tolk.psi.TolkTypedElement
import org.ton.intellij.tolk.psi.reference.TolkTypeReference
import org.ton.intellij.tolk.type.TolkType

abstract class TolkReferenceTypeExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node),
    TolkReferenceTypeExpression {
    private val primitiveType: TolkType? get() = TolkType.byName(this.text)

    val isPrimitive get() = primitiveType != null

    override val type: TolkType?
        get() {
            val primitiveType = primitiveType
            if (primitiveType != null) return primitiveType

            val resolved = reference?.resolve()
            if (resolved is TolkTypedElement) {
                return resolved.type
            }
            if (parentOfType<TolkFunctionReceiver>() != null) {
                return TolkType.GenericType(this)
            }
            return null
        }

    override fun getReference() = if(isPrimitive) null else TolkTypeReference(this)


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
