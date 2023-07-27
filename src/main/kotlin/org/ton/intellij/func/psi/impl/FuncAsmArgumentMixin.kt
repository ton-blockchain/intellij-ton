package org.ton.intellij.func.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.func.psi.FuncAsmArgument
import org.ton.intellij.func.psi.FuncElementFactory
import org.ton.intellij.func.psi.FuncFunction

abstract class FuncAsmArgumentMixin(node: ASTNode) : ASTWrapperPsiElement(node), FuncAsmArgument {
    override fun getReferences(): Array<PsiReference> = arrayOf(FuncAsmArgumentReference(this))

    override fun getReference(): PsiReference? = references.firstOrNull()
}

class FuncAsmArgumentReference(element: FuncAsmArgument) :
    PsiReferenceBase<FuncAsmArgument>(element, TextRange.from(0, element.textLength)) {
    private val resolver = ResolveCache.AbstractResolver<PsiReferenceBase<FuncAsmArgument>, PsiElement> { r, _ ->
        if (!r.element.isValid) return@AbstractResolver null
        val function =
            PsiTreeUtil.getParentOfType(myElement, FuncFunction::class.java) ?: return@AbstractResolver null
        val params = function.functionParameterList
        return@AbstractResolver params.find {
            val name = it.name ?: return@AbstractResolver null
            myElement.identifier.textMatches(name)
        }
    }

    override fun resolve(): PsiElement? {
        if (!element.isValid) return null
        return ResolveCache.getInstance(myElement.project).resolveWithCaching(this, resolver, false, false)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        myElement.identifier.replace(FuncElementFactory[myElement.project].createIdentifierFromText(newElementName))
        return myElement
    }
}
