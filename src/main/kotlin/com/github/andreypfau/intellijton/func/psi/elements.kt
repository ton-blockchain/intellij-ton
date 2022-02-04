package com.github.andreypfau.intellijton.func.psi

import com.github.andreypfau.intellijton.func.FuncIcons
import com.github.andreypfau.intellijton.parentOfType
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import javax.swing.Icon

interface FuncElement : PsiElement {
    override fun getReference(): PsiReference? = null

}

open class FuncElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), FuncElement {
    override fun getReference(): PsiReference? = null
}

interface FuncSourceUnit : FuncElement

interface FuncNamedElement : FuncElement, PsiNamedElement, NavigatablePsiElement
interface FuncNamedIdentifierElement : FuncNamedElement, PsiNameIdentifierOwner
open class FuncNamedElementImpl(node: ASTNode) : FuncElementImpl(node), FuncNamedIdentifierElement {
    override fun getNameIdentifier(): PsiElement? = findChildByType(FuncTypes.IDENTIFIER)
    override fun getName(): String? = nameIdentifier?.text
    override fun setName(name: String): PsiElement = apply {
        nameIdentifier?.replace(project.funcPsiFactory.createIdentifier(name))
    }

    override fun getNavigationElement(): PsiElement = nameIdentifier ?: this
    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}

open class FuncFunctionDefinitionMixin(node: ASTNode) : FuncNamedElementImpl(node) {
    var isBuiltIn: Boolean = false

    override fun canNavigate(): Boolean = !isBuiltIn
    override fun canNavigateToSource(): Boolean = !isBuiltIn

    override fun getIcon(flags: Int): Icon = FuncIcons.FUNCTION

    override fun getPresentation(): ItemPresentation? {
        val name = name ?: return null
        return PresentationData(
            name,
            null,
            FuncIcons.FUNCTION,
            TextAttributesKey.createTextAttributesKey("public")
        )
    }
}

open class FuncFunctionCallMixin(node: ASTNode) : FuncNamedElementImpl(node) {
    override fun getReference() = object : PsiReferenceBase<FuncFunctionCallMixin>(this) {
        override fun resolve(): PsiElement? =
            element.parentOfType<FuncSourceUnit>()?.childrenOfType<FuncFunctionDefinition>()?.find {
                it.identifier.textMatches(element)
            }
    }
}

