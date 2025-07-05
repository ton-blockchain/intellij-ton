package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tolk.psi.TolkIncludeDefinition
import org.ton.intellij.tolk.stub.TolkIncludeDefinitionStub
import org.ton.intellij.util.greenStub

abstract class TolkIncludeDefinitionMixin : StubBasedPsiElementBase<TolkIncludeDefinitionStub>, TolkIncludeDefinition {
    constructor(stub: TolkIncludeDefinitionStub, type: IStubElementType<*, *>) : super(stub, type)
    constructor(node: ASTNode) : super(node)
    constructor(stub: TolkIncludeDefinitionStub?, type: IElementType?, node: ASTNode?) : super(stub, type, node)

    override fun getTextOffset(): Int {
        val stringLiteral = stringLiteral
        return if (stringLiteral != null) {
            stringLiteral.startOffsetInParent + (stringLiteral.rawString?.startOffsetInParent
                ?: return super.getTextOffset())
        } else {
            super.getTextOffset()
        }
    }

    fun resolve(): PsiElement? {
        return stringLiteral?.references?.lastOrNull()?.resolve()
    }
}

val TolkIncludeDefinition.path: String
    get() = greenStub?.path ?: stringLiteral?.rawString?.text ?: ""

fun TolkIncludeDefinition.resolve() = (this as TolkIncludeDefinitionMixin).resolve()
