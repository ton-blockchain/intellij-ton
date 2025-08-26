package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkSelfTypeExpression
import org.ton.intellij.tolk.psi.TolkTypeExpression
import org.ton.intellij.tolk.stub.TolkTypeStub
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkSelfTypeExpressionMixin : TolkStubbedElementImpl<TolkTypeStub<*>>, TolkSelfTypeExpression {
    constructor(node: ASTNode) : super(node)
    constructor(stub: TolkTypeStub<*>, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val type: TolkTy
        get() = reference.resolve()?.type ?: TolkTy.Unknown

    private val reference = SelfTypeReference(this)

    override fun getReference() = reference

    class SelfTypeReference(
        element: TolkSelfTypeExpression
    ) : PsiReferenceBase<TolkSelfTypeExpression>(element) {

        override fun resolve(): TolkTypeExpression? =
            ResolveCache.getInstance(element.project).resolveWithCaching(this, Resolver(), false, false)

        override fun calculateDefaultRangeInElement(): TextRange =
            TextRange(0, element.textLength)

        private inner class Resolver() : ResolveCache.AbstractResolver<SelfTypeReference, TolkTypeExpression?> {
            override fun resolve(
                ref: SelfTypeReference,
                incompleteCode: Boolean
            ): TolkTypeExpression? {
                val function = element.parentOfType<TolkFunction>()
                return function?.functionReceiver?.typeExpression
            }
        }
    }
}
