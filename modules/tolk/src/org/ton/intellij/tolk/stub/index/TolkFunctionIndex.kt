package org.ton.intellij.tolk.stub.index

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.tolk.TolkFileElementType
import org.ton.intellij.tolk.psi.TolkFunction

class TolkFunctionIndex : StringStubIndexExtension<TolkFunction>() {
    override fun getVersion(): Int = TolkFileElementType.stubVersion

    override fun getKey(): StubIndexKey<String, TolkFunction> = KEY

    companion object {
        val KEY: StubIndexKey<String, TolkFunction> =
            StubIndexKey.createIndexKey("org.ton.intellij.tolk.stub.index.TolkFunctionIndex")
    }
}
