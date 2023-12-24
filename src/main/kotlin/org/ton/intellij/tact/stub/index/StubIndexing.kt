package org.ton.intellij.tact.stub.index

import com.intellij.psi.stubs.IndexSink
import org.ton.intellij.tact.stub.TactFunctionStub
import org.ton.intellij.tact.stub.TactNamedStub

fun IndexSink.indexFunction(stub: TactFunctionStub) {
    indexNamedStub(stub)
}

private fun IndexSink.indexNamedStub(stub: TactNamedStub<*>) {
    stub.name?.let {
        occurrence(TactNamedElementIndex.KEY, it)
    }
}
