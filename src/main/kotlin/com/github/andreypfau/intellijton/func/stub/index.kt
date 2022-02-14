package com.github.andreypfau.intellijton.func.stub

import com.github.andreypfau.intellijton.func.psi.FuncNamedElement
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class FuncNamedElementIndex : StringStubIndexExtension<FuncNamedElement>() {
    override fun getVersion(): Int = FuncFileStub.Type.stubVersion
    override fun getKey(): StubIndexKey<String, FuncNamedElement> = KEY

    companion object {
        val KEY: StubIndexKey<String, FuncNamedElement> =
            StubIndexKey.createIndexKey(FuncNamedElementIndex::class.java.canonicalName)
    }
}

class FuncFunctionIndex : StringStubIndexExtension<FuncNamedElement>() {
    override fun getVersion(): Int = FuncFileStub.Type.stubVersion
    override fun getKey(): StubIndexKey<String, FuncNamedElement> = KEY

    companion object {
        val KEY: StubIndexKey<String, FuncNamedElement> =
            StubIndexKey.createIndexKey(FuncFunctionIndex::class.java.canonicalName)
    }
}