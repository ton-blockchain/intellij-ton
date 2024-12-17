package org.ton.intellij.fc2tolk.tree

import org.ton.intellij.fc2tolk.tree.visitors.FTVisitor

abstract class FTDeclaration : FTTreeElement() {
    abstract val name: FTNameIdentifier
}


class FTFunction(
    returnType: FTTypeElement,
    name: FTNameIdentifier,
    typeParameterList: FTTypeParameterList,
    functionModifiers: List<FTFunctionModifier>
) : FTDeclaration() {
    val returnType: FTTypeElement by child(returnType)
    override val name: FTNameIdentifier by child(name)
    val typeParameterList: FTTypeParameterList by child(typeParameterList)
    val functionModifiers: List<FTFunctionModifier> by children(functionModifiers)

    override fun accept(visitor: FTVisitor) = visitor.visitFunction(this)
}

class FTTypeParameter(
    name: FTNameIdentifier
) : FTDeclaration() {
    override val name: FTNameIdentifier by child(name)

    override fun accept(visitor: FTVisitor) = visitor.visitTypeParameter(this)
}

class FTTypeParameterList(
    typeParameters: List<FTTypeParameter>
) : FTTreeElement() {
    var typeParameters: List<FTTypeParameter> by children(typeParameters)

    override fun accept(visitor: FTVisitor) = visitor.visitTypeParameterList(this)
}