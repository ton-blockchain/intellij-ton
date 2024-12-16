package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.tolk.psi.TolkAsmArgument
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkPsiFactory

abstract class TolkAsmArgumentMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkAsmArgument {
    override fun getReferences(): Array<PsiReference> = arrayOf(TolkAsmArgumentReference(this))

    override fun getReference(): PsiReference? = references.firstOrNull()
}

class TolkAsmArgumentReference(element: TolkAsmArgument) :
    PsiReferenceBase<TolkAsmArgument>(element, TextRange.from(0, element.textLength)) {
    private val resolver = ResolveCache.AbstractResolver<PsiReferenceBase<TolkAsmArgument>, PsiElement> { r, _ ->
        if (!r.element.isValid) return@AbstractResolver null
        val function =
            PsiTreeUtil.getParentOfType(myElement, TolkFunction::class.java) ?: return@AbstractResolver null
        val params = function.parameterList?.parameterList ?: return@AbstractResolver null
        return@AbstractResolver params.find {
            val name = it.name ?: return@find false
            myElement.identifier?.textMatches(name) ?: false
        }
    }

    override fun resolve(): PsiElement? {
        if (!element.isValid) return null
        return ResolveCache.getInstance(myElement.project).resolveWithCaching(this, resolver, false, false)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        myElement.identifier?.replace(TolkPsiFactory[myElement.project].createIdentifier(newElementName))
        return myElement
    }
}
