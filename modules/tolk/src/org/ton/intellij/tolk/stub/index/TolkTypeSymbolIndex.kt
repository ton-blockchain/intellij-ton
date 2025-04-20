package org.ton.intellij.tolk.stub.index

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.tolk.TolkFileElementType
import org.ton.intellij.tolk.psi.TolkTypeSymbolElement

class TolkTypeSymbolIndex : StringStubIndexExtension<TolkTypeSymbolElement>() {
    override fun getVersion(): Int = TolkFileElementType.stubVersion

    override fun getKey(): StubIndexKey<String, TolkTypeSymbolElement> = KEY

    companion object {
        val KEY: StubIndexKey<String, TolkTypeSymbolElement> =
            StubIndexKey.createIndexKey("org.ton.intellij.tolk.stub.index.TolkTypeSymbolIndex")
    }
}
