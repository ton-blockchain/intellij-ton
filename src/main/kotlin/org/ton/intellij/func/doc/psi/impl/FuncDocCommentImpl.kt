package org.ton.intellij.func.doc.psi.impl

import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.func.doc.psi.FuncDocCodeFence
import org.ton.intellij.func.doc.psi.FuncDocComment
import org.ton.intellij.func.psi.FuncDocOwner

class FuncDocCommentImpl(
    type: IElementType, text: CharSequence?,
) : LazyParseablePsiElement(type, text), FuncDocComment {

    override fun getTokenType(): IElementType = elementType

    override fun getOwner(): FuncDocOwner? = PsiTreeUtil.getParentOfType(this, FuncDocOwner::class.java, true)


    override fun getReferences(): Array<PsiReference> = ReferenceProvidersRegistry.getReferencesFromProviders(this)

    override fun accept(visitor: PsiElementVisitor) {
        visitor.visitComment(this)
    }

    override fun toString(): String = "PsiComment($elementType)"

    override val codeFences: List<FuncDocCodeFence>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, FuncDocCodeFence::class.java)

}
