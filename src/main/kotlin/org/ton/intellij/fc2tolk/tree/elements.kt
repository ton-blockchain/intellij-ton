package org.ton.intellij.fc2tolk.tree

import org.ton.intellij.fc2tolk.tree.visitors.FTVisitor

class FTTreeRoot(element: FTTreeElement) : FTTreeElement() {
    var element by child(element)

    override fun accept(visitor: FTVisitor) = visitor.visitTreeRoot(this)
}

class FTFile(
    declarationList: List<FTDeclaration>
) : FTTreeElement() {
    var declarationList: List<FTDeclaration> by children(declarationList)

    override fun accept(visitor: FTVisitor) = visitor.visitFile(this)
}

class FTNameIdentifier(val value: String) : FTTreeElement() {
    override fun accept(visitor: FTVisitor) = visitor.visitNameIdentifier(this)
}

class FTTypeElement(val type: FTType) : FTTreeElement() {
    override fun accept(visitor: FTVisitor) = visitor.visitTypeElement(this)
}

abstract class FTFunctionModifier : FTTreeElement()

class FTMethodId(val value: String?) : FTFunctionModifier() {
    override fun accept(visitor: FTVisitor) = visitor.visitMethodId(this)
}