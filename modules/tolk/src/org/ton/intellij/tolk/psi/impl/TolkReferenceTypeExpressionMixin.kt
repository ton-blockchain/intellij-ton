package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.*
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
            return null
        }

    override fun getReferences(): Array<PsiReference> {
        if (isPrimitive) return PsiReference.EMPTY_ARRAY
        return arrayOf(TolkTypeIdentifierReference(this))
    }

    override fun getReference(): PsiReference? = references.firstOrNull()

    class TolkTypeIdentifierReference(element: TolkReferenceTypeExpression) :
        PsiReferenceBase.Poly<TolkReferenceTypeExpression>(
            element, TextRange(0, element.textLength), false
        ) {
        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            return buildList<ResolveResult> {
                val typeParameterName = myElement.identifier.text.removeSurrounding("`")
                val owner = myElement.parentOfType<TolkTypeParameterListOwner>()
                if (owner != null) {
                    if (typeParameterName == "self" && owner is TolkFunction) {
                        val selfParameter = owner.parameterList?.parameterList?.find { it.name == "self" }
                        if (selfParameter != null) {
                            add(PsiElementResolveResult(selfParameter))
                        }
                    }

                    val typeParameterList = owner.typeParameterList
                    typeParameterList?.typeParameterList?.forEach { typeParameter ->
                        if (typeParameter.name == typeParameterName) {
                            add(PsiElementResolveResult(typeParameter))
                        }
                    }
                }

                val file = myElement.containingFile as? TolkFile ?: return@buildList
                val typeDefResults = collectTypeDefResults(file, typeParameterName)
                addAll(typeDefResults)
            }.toTypedArray()
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

private fun collectTypeDefResults(file: TolkFile, target: String): List<PsiElementResolveResult> {
    val results = ArrayList<PsiElementResolveResult>()

    val includedFiles = file.collectIncludedFiles(true)
    includedFiles.forEach { file ->
        file.typeDefs.forEach { typeDef ->
            if (typeDef.name == target) {
                results.add(PsiElementResolveResult(typeDef))
            }
        }
        file.structs.forEach { struct ->
            if (struct.name == target) {
                results.add(PsiElementResolveResult(struct))
            }
        }
    }
    return results
}

val TolkReferenceTypeExpression.isPrimitive: Boolean
    get() = (this as TolkReferenceTypeExpressionMixin).isPrimitive
