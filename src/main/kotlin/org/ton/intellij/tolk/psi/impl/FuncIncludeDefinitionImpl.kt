package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tolk.psi.TolkIncludeDefinition
import org.ton.intellij.tolk.stub.TolkIncludeDefinitionStub

abstract class TolkIncludeDefinitionMixin : StubBasedPsiElementBase<TolkIncludeDefinitionStub>, TolkIncludeDefinition {
    constructor(stub: TolkIncludeDefinitionStub, type: IStubElementType<*, *>) : super(stub, type)
    constructor(node: ASTNode) : super(node)
    constructor(stub: TolkIncludeDefinitionStub?, type: IElementType?, node: ASTNode?) : super(stub, type, node)

    override fun getReferences(): Array<FileReference> {
        return TolkIncludePathReference(this).allReferences
    }

    override fun getReference(): PsiReference? = references.lastOrNull()

    override fun getTextOffset(): Int {
        val stringLiteral = stringLiteral
        return if (stringLiteral != null) {
            stringLiteral.startOffsetInParent + (stringLiteral.rawString?.startOffsetInParent
                ?: return super.getTextOffset())
        } else {
            super.getTextOffset()
        }
    }
}

val TolkIncludeDefinition.path: String
    get() = stub?.path ?: stringLiteral?.rawString?.text ?: ""
