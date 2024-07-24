package org.ton.intellij.tlb.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import org.ton.intellij.tlb.TlbLanguage
import org.ton.intellij.tlb.psi.TlbElement

abstract class TlbStubElementType<StubT : StubElement<*>, PsiT : TlbElement>(
    debugName: String
) : IStubElementType<StubT, PsiT>(debugName, TlbLanguage) {

    final override fun getExternalId(): String = "tlb.${super.toString()}"

    override fun indexStub(stub: StubT, sink: IndexSink) {}
}
