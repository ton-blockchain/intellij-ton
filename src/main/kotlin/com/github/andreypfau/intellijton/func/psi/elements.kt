package com.github.andreypfau.intellijton.func.psi

import com.github.andreypfau.intellijton.func.FuncIcons
import com.github.andreypfau.intellijton.func.resolve.FuncReference
import com.github.andreypfau.intellijton.func.resolve.FuncVarLiteralReference
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import javax.swing.Icon

interface FuncElement : PsiElement

open class FuncElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), FuncElement {
}

interface FuncNamedElement : FuncElement, PsiNameIdentifierOwner
interface FuncReferenceElement : FuncNamedElement {
    val referenceNameElement: PsiElement
    val referenceName: String
        get() = referenceNameElement.text

    override fun getReference(): FuncReference?
}

abstract class FuncNamedElementImpl(node: ASTNode) : FuncElementImpl(node), FuncNamedElement {
    override fun getNameIdentifier(): PsiElement? = findChildByType(FuncTypes.IDENTIFIER)
    override fun getName(): String? = nameIdentifier?.text
    override fun setName(name: String): PsiElement = apply {
        nameIdentifier?.replace(project.funcPsiFactory.createIdentifier(name))
    }

    override fun getNavigationElement(): PsiElement = nameIdentifier ?: this
    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}

interface FuncFunctionCallElement : FuncReferenceElement {
}

abstract class FuncFunctionCallMixin(
    node: ASTNode
) : FuncNamedElementImpl(node), FuncFunctionCallElement, FuncFunctionCallExpression {
    override val referenceNameElement: PsiElement
        get() = when (val expr = expression) {
            is FuncPrimaryExpression -> expr.varLiteral ?: expr.primitiveTypeName!!
            is FuncMemberAccessExpression -> expr.memberAccessIdentifier
            is FuncModifierAccessExpression -> expr.firstChild
            is FuncFunctionCallElement -> expr.referenceNameElement
            else -> error("Can't extract reference name element for expression: $expr")
        }

    override fun getReference(): FuncReference? = null
}

interface FuncVarLiteralElement : FuncReferenceElement
abstract class FuncVarLiteralMixin(
    node: ASTNode
) : FuncNamedElementImpl(node), FuncVarLiteralElement, FuncVarLiteral {
    override val referenceNameElement: PsiElement
        get() = nameIdentifier!!

    override fun getReference() = FuncVarLiteralReference(this)
}

abstract class FuncFunctionDefinitionMixin(node: ASTNode) : FuncNamedElementImpl(node), FuncFunctionDefinition {
    var isBuiltIn: Boolean = false
    override fun getNameIdentifier(): PsiElement? = functionIdentifier.identifier
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