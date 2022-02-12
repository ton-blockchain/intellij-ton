package com.github.andreypfau.intellijton.func.psi

import com.github.andreypfau.intellijton.func.FuncIcons
import com.github.andreypfau.intellijton.func.resolve.FuncFunctionCallReference
import com.github.andreypfau.intellijton.func.resolve.FuncReference
import com.github.andreypfau.intellijton.func.stub.FuncFunctionDefinitionStub
import com.github.andreypfau.intellijton.func.stub.FuncStubbedNamedElementImpl
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.stubs.IStubElementType
import javax.swing.Icon

interface FuncElement : PsiElement
abstract class FuncElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), FuncElement

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

abstract class FuncFunctionDefinitionMixin : FuncStubbedNamedElementImpl<FuncFunctionDefinitionStub>, FuncFunctionDefinition {
    constructor(node: ASTNode) : super(node)
    constructor(stub: FuncFunctionDefinitionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    var isBuiltIn: Boolean = false
    override fun getNameIdentifier(): PsiElement? = functionName.identifier
    override fun canNavigate(): Boolean = !isBuiltIn
    override fun canNavigateToSource(): Boolean = !isBuiltIn

    override fun getIcon(flags: Int): Icon = FuncIcons.FUNCTION

    override fun getPresentation(): PresentationData {
        val name = name
        return PresentationData(
            name,
            null,
            FuncIcons.FUNCTION,
            TextAttributesKey.createTextAttributesKey("public")
        )
    }
}

abstract class FuncFunctionCallMixin(
    node: ASTNode
)  : FuncNamedElementImpl(node), FuncFunctionCall {
    override val referenceNameElement: PsiElement get() = identifier
    override fun getReference() = FuncFunctionCallReference(this)
}