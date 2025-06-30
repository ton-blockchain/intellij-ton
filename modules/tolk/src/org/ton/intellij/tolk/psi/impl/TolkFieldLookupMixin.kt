package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkFieldLookup
import org.ton.intellij.tolk.psi.TolkTypeArgumentList
import org.ton.intellij.tolk.psi.reference.TolkFieldLookupReference

abstract class TolkFieldLookupMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkFieldLookup {
    override val typeArgumentList: TolkTypeArgumentList?
        get() = findChildByClass(TolkTypeArgumentList::class.java)

    override val referenceNameElement: PsiElement? get() = identifier ?: integerLiteral

    override fun getReference(): TolkFieldLookupReference? {
        return TolkFieldLookupReference(this)
    }
}

val TolkFieldLookup.parentDotExpression: TolkDotExpression
    get() = parent as TolkDotExpression
