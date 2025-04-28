package org.ton.intellij.tolk.stub.type

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubBase
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.psi.TolkElement

abstract class TolkStubElementType<S : StubBase<T>, T : TolkElement>(
    debugName: String
) : IStubElementType<S, T>(debugName, TolkLanguage) {
    override fun getExternalId(): String = "tolk.${super.toString()}"

    override fun indexStub(stub: S, sink: IndexSink) {
    }
}
