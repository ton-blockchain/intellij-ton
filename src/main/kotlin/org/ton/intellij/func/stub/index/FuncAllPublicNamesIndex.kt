package org.ton.intellij.func.stub.index

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.func.FuncFileElementType
import org.ton.intellij.func.psi.FuncNamedElement

class FuncAllPublicNamesIndex : StringStubIndexExtension<FuncNamedElement>() {
    override fun getKey(): StubIndexKey<String, FuncNamedElement> = ALL_PUBLIC_NAMES

    override fun getVersion(): Int = FuncFileElementType.stubVersion

    companion object {
        val ALL_PUBLIC_NAMES: StubIndexKey<String, FuncNamedElement> =
            StubIndexKey.createIndexKey<String, FuncNamedElement>("func.all.name")
    }
}
