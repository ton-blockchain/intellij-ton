package org.ton.intellij.tolk.doc.psi.impl

import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.tolk.doc.psi.TolkDocCodeFence
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.psi.TolkDocOwner

class TolkDocCommentImpl(
    type: IElementType, text: CharSequence?,
) : LazyParseablePsiElement(type, text), TolkDocComment {

    override fun getTokenType(): IElementType = elementType

    override fun getOwner(): TolkDocOwner? = PsiTreeUtil.getParentOfType(this, TolkDocOwner::class.java, true)

    override fun getReferences(): Array<PsiReference> = ReferenceProvidersRegistry.getReferencesFromProviders(this)

    override fun accept(visitor: PsiElementVisitor) {
        visitor.visitComment(this)
    }

    override fun toString(): String = "PsiComment($elementType)"

    override val codeFences: List<TolkDocCodeFence>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, TolkDocCodeFence::class.java)

}
