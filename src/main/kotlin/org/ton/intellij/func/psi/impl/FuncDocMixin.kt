package org.ton.intellij.func.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.ton.intellij.func.psi.FuncDoc
import org.ton.intellij.func.psi.FuncElementTypes

abstract class FuncDocMixin(node: ASTNode) : ASTWrapperPsiElement(node), FuncDoc {
    override fun getOwner(): PsiElement? = parent

    override fun getTokenType(): IElementType = FuncElementTypes.DOC_ELEMENT
}
