package org.ton.intellij.tolk.stub.type

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.tolk.psi.TolkNamedElement
import org.ton.intellij.tolk.stub.TolkNamedStub
import org.ton.intellij.tolk.stub.index.TolkNamedElementIndex

abstract class TolkNamedStubElementType<S : TolkNamedStub<T>, T : TolkNamedElement>(
    debugName: String
) : TolkStubElementType<S, T>(debugName) {
    protected open val extraIndexKeys: Collection<StubIndexKey<String, out TolkNamedElement>> = emptyList()

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        if (!super.shouldCreateStub(node)) return false
        val psi = node?.psi ?: return false
        return psi is TolkNamedElement && !psi.name.isNullOrEmpty()
    }

    override fun indexStub(stub: S, sink: IndexSink) {
        val name = stub.name?.removeSurrounding("`")
        if (name.isNullOrEmpty() || !shouldIndex()) return

        sink.occurrence(TolkNamedElementIndex.KEY, name)
        extraIndexKeys.forEach { key ->
            sink.occurrence(key, name)
        }
    }

    protected fun shouldIndex(): Boolean = true
}
