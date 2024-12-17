package org.ton.intellij.func.stub.type

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubBase
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.psi.FuncElement

abstract class FuncStubElementType<S : StubBase<T>, T : FuncElement>(
    debugName: String
) : IStubElementType<S, T>(debugName, FuncLanguage) {
    override fun getExternalId(): String = "func.${super.toString()}"

    override fun indexStub(stub: S, sink: IndexSink) {
    }

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return super.shouldCreateStub(node)
    }
}
