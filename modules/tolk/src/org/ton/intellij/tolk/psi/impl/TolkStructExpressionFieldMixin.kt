package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.tolk.psi.TolkStructExpression
import org.ton.intellij.tolk.psi.TolkStructExpressionField
import org.ton.intellij.tolk.type.TolkStructType

abstract class TolkStructExpressionFieldMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkStructExpressionField {
    override fun getReferences(): Array<out PsiReference> {
        return arrayOf(FieldReference(this))
    }

    class FieldReference(val field: TolkStructExpressionField) : PsiReferenceBase.Poly <TolkStructExpressionField>(field, field.identifier.textRangeInParent, false) {
        val identifier: PsiElement get() = element.identifier

        private val resolver = ResolveCache.PolyVariantResolver<FieldReference> { t, incompleteCode ->
            if (field.node.findChildByType(TolkElementTypes.COLON) == null) {
                // resolve local variable
                return@PolyVariantResolver ResolveResult.EMPTY_ARRAY
            }
            val structExpression = field.parentOfType<TolkStructExpression>() ?: return@PolyVariantResolver ResolveResult.EMPTY_ARRAY

            val structType =
                structExpression.type?.unwrapTypeAlias() as? TolkStructType ?: return@PolyVariantResolver ResolveResult.EMPTY_ARRAY

            val name = field.identifier.text.removeSurrounding("`")
            val field = structType.psi?.structBody?.structFieldList?.find {
                it.name == name
            } ?: return@PolyVariantResolver ResolveResult.EMPTY_ARRAY
            return@PolyVariantResolver arrayOf(
                PsiElementResolveResult(field)
            )
        }

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            if (!myElement.isValid) return ResolveResult.EMPTY_ARRAY
            return resolver.resolve(this, incompleteCode)
//            return ResolveCache.getInstance(myElement.project).resolveWithCaching(this, resolver, false, incompleteCode)
        }

        override fun handleElementRename(newElementName: String): PsiElement {
            return identifier.replace(TolkPsiFactory[element.project].createIdentifier(newElementName))
        }
    }
}
