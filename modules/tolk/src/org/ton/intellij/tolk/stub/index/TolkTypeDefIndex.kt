package org.ton.intellij.tolk.stub.index

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.tolk.TolkFileElementType
import org.ton.intellij.tolk.psi.TolkTypeDef

class TolkTypeDefIndex : StringStubIndexExtension<TolkTypeDef>() {
    override fun getVersion(): Int = TolkFileElementType.stubVersion

    override fun getKey(): StubIndexKey<String, TolkTypeDef> = KEY

    companion object {
        val KEY: StubIndexKey<String, TolkTypeDef> =
            StubIndexKey.createIndexKey("org.ton.intellij.tolk.stub.index.TolkTypeDefIndex")
    }
}
