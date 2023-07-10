package org.ton.intellij.func.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.IElementType
import org.ton.intellij.func.psi.FuncIncludeDefinition
import org.ton.intellij.func.stub.FuncIncludeDefinitionStub

abstract class FuncIncludeDefinitionMixin : StubBasedPsiElementBase<FuncIncludeDefinitionStub>, FuncIncludeDefinition {
    constructor(stub: FuncIncludeDefinitionStub, type: IStubElementType<*, *>) : super(stub, type)
    constructor(node: ASTNode) : super(node)
    constructor(stub: FuncIncludeDefinitionStub?, type: IElementType?, node: ASTNode?) : super(stub, type, node)

    override fun getReferences(): Array<FileReference> {
        return FuncIncludePathReference(this).allReferences
    }

    override fun getTextOffset(): Int = stringLiteral.startOffsetInParent + stringLiteral.rawString.startOffsetInParent
}

val FuncIncludeDefinition.path: String
    get() = stub?.path ?: stringLiteral.rawString.text
