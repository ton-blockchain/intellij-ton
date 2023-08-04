package org.ton.intellij.func.stub.type

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.func.psi.FuncNamedElement
import org.ton.intellij.func.stub.FuncNamedStub

abstract class FuncNamedStubElementType<S : FuncNamedStub<T>, T : FuncNamedElement>(
    debugName: String
) : FuncStubElementType<S, T>(debugName) {
    protected val extraIndexKeys: Collection<StubIndexKey<String, out FuncNamedElement>> = emptyList()

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        if (!super.shouldCreateStub(node)) return false
        val psi = node?.psi ?: return false
        return psi is FuncNamedElement && !psi.name.isNullOrEmpty()
    }

    override fun indexStub(stub: S, sink: IndexSink) {
        val name = stub.name
        if (name.isNullOrEmpty() || !shouldIndex()) return

//        sink.occurrence(FuncAllPublicNamesIndex.ALL_PUBLIC_NAMES, name)

        extraIndexKeys.forEach { key ->
            sink.occurrence(key, name)
        }
    }

    protected fun shouldIndex(): Boolean = true
}
