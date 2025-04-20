package org.ton.intellij.tolk.stub.index

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.tolk.TolkFileElementType
import org.ton.intellij.tolk.psi.TolkStruct

class TolkStructIndex : StringStubIndexExtension<TolkStruct>() {
    override fun getVersion(): Int = TolkFileElementType.stubVersion

    override fun getKey(): StubIndexKey<String, TolkStruct> = KEY

    companion object {
        val KEY: StubIndexKey<String, TolkStruct> =
            StubIndexKey.createIndexKey("org.ton.intellij.tolk.stub.index.TolkStructIndex")
    }
}
