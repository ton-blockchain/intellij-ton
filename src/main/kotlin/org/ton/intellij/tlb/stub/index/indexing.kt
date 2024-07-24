package org.ton.intellij.tlb.stub.index

import com.intellij.psi.stubs.IndexSink
import org.ton.intellij.tlb.stub.TlbConstructorStub

fun IndexSink.indexTlbConstructor(stub: TlbConstructorStub) {
    stub.name?.let {
        occurrence(TlbNamedElementIndex.KEY, it)
    }
}
