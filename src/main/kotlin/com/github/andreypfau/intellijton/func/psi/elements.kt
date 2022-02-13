package com.github.andreypfau.intellijton.func.psi

import com.github.andreypfau.intellijton.func.FuncIcons
import com.github.andreypfau.intellijton.func.resolve.FuncFunctionCallIdentifierReference
import com.github.andreypfau.intellijton.func.resolve.FuncFunctionCallReference
import com.github.andreypfau.intellijton.func.stub.FuncFunctionDefinitionStub
import com.github.andreypfau.intellijton.func.stub.FuncStubbedNamedElementImpl
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.stubs.IStubElementType
import javax.swing.Icon

interface FuncElement : PsiElement
abstract class FuncElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), FuncElement
interface FuncNamedElement : FuncElement, PsiNameIdentifierOwner

abstract class FuncNamedElementImpl(node: ASTNode) : FuncElementImpl(node), FuncNamedElement {
    override fun getNameIdentifier(): PsiElement? = findChildByType(FuncTypes.IDENTIFIER)
    override fun getName(): String? = nameIdentifier?.text
    override fun setName(name: String): PsiElement = apply {
        nameIdentifier?.replace(project.funcPsiFactory.createIdentifier(name))
    }
    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}

abstract class FuncFunctionDefinitionMixin : FuncStubbedNamedElementImpl<FuncFunctionDefinitionStub>, FuncFunctionDefinition {
    constructor(node: ASTNode) : super(node)
    constructor(stub: FuncFunctionDefinitionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    override fun getNameIdentifier(): PsiElement? = functionName.identifier
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

abstract class FuncFunctionCallMixin(node: ASTNode) : FuncNamedElementImpl(node), FuncFunctionCall {
    override fun getNameIdentifier() = functionCallIdentifier.identifier
    override fun getReference() = FuncFunctionCallReference(this)
}

abstract class FuncFunctionCallIdentifierMixin(node: ASTNode) : FuncNamedElementImpl(node), FuncFunctionCallIdentifier {
    override fun getNameIdentifier() = identifier
    override fun getReference() = FuncFunctionCallIdentifierReference(this)
}