package com.github.andreypfau.intellijton.func.psi

import com.github.andreypfau.intellijton.func.FuncIcons
import com.github.andreypfau.intellijton.func.resolve.*
import com.github.andreypfau.intellijton.func.stub.FuncFunctionStub
import com.github.andreypfau.intellijton.func.stub.FuncStubbedNamedElementImpl
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.stubs.IStubElementType

interface FuncElement : PsiElement
abstract class FuncElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), FuncElement
interface FuncNamedElement : FuncElement, PsiNameIdentifierOwner {
    override fun getReference(): FuncReferenceBase<*>?
}

abstract class FuncNamedElementImpl(node: ASTNode) : FuncElementImpl(node), FuncNamedElement {
    override fun getNameIdentifier(): PsiElement? = findChildByType(FuncTokenTypes.IDENTIFIER)
    override fun getName() = nameIdentifier?.text
    override fun setName(name: String) = apply {
        nameIdentifier?.replace(project.funcPsiFactory.createIdentifier(name))
    }

    override fun getTextOffset() = nameIdentifier?.textOffset ?: super.getTextOffset()
    override fun getReference(): FuncReferenceBase<*>? = null
}

abstract class FuncFunctionDefinitionMixin : FuncStubbedNamedElementImpl<FuncFunctionStub>,
    FuncFunction {
    constructor(node: ASTNode) : super(node)
    constructor(stub: FuncFunctionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getNameIdentifier() = functionName.identifier
    override fun getIcon(flags: Int) = FuncIcons.FUNCTION
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

abstract class FuncParameterDeclarationMixin(node: ASTNode) : FuncNamedElementImpl(node), FuncParameterDeclaration {
    override fun getNameIdentifier(): PsiElement = identifier
    override fun getIcon(flags: Int) = FuncIcons.PARAMETER
    override fun getPresentation(): PresentationData {
        val name = name
        return PresentationData(
            name,
            null,
            FuncIcons.VARIABLE,
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

abstract class FuncMethodCallMixin(node: ASTNode) : FuncNamedElementImpl(node), FuncMethodCall {
    override fun getNameIdentifier() = methodCallIdentifier?.identifier
    override fun getReference() = FuncMethodCallReference(this)
}

abstract class FuncMethodCallIdentifierMixin(node: ASTNode) : FuncNamedElementImpl(node), FuncMethodCallIdentifier {
    override fun getNameIdentifier() = identifier
    override fun getReference() = FuncMethodCallIdentifierReference(this)
}

abstract class FuncModifyingMethodCallMixin(node: ASTNode) : FuncNamedElementImpl(node), FuncModifyingMethodCall {
    override fun getNameIdentifier() = modifyingMethodCallIdentifier.identifier
    override fun getReference() = FuncModifyingMethodCallReference(this)
}

abstract class FuncModifyingMethodCallIdentifierMixin(node: ASTNode) : FuncNamedElementImpl(node),
    FuncModifyingMethodCallIdentifier {
    override fun getNameIdentifier() = identifier
    override fun getReference() = FuncModifyingMethodCallIdentifierReference(this)
}

abstract class FuncGlobalVarMixin(node: ASTNode) : FuncNamedElementImpl(node), FuncGlobalVar {
    override fun getNameIdentifier() = identifier
}

abstract class FuncConstDeclarationMixin(node: ASTNode) : FuncNamedElementImpl(node), FuncConstDeclaration {
    override fun getNameIdentifier() = identifier
    override fun getIcon(flags: Int) = FuncIcons.CONSTANT
    override fun getPresentation(): PresentationData {
        val name = name
        return PresentationData(
            name,
            null,
            FuncIcons.VARIABLE,
            TextAttributesKey.createTextAttributesKey("public")
        )
    }
}

abstract class FuncVariableDeclarationMixin(node: ASTNode) : FuncNamedElementImpl(node), FuncVariableDeclaration {
    override fun getNameIdentifier() = identifier
    override fun getIcon(flags: Int) = FuncIcons.VARIABLE
    override fun getPresentation(): PresentationData {
        val name = name
        return PresentationData(
            name,
            null,
            FuncIcons.VARIABLE,
            TextAttributesKey.createTextAttributesKey("public")
        )
    }
}

abstract class FuncReferenceExpressionMixin(node: ASTNode) : FuncNamedElementImpl(node), FuncReferenceExpression {
    override fun getNameIdentifier() = identifier
    override fun getReference() = FuncReferenceExpressionReference(this)
}