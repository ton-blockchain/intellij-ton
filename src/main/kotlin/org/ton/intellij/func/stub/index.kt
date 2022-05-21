package org.ton.intellij.func.stub

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.FuncIncludePath
import org.ton.intellij.func.psi.FuncNamedElement

class FuncFileIndex : StringStubIndexExtension<FuncFile>() {
    override fun getVersion(): Int = FuncFileStub.Type.stubVersion
    override fun getKey(): StubIndexKey<String, FuncFile> = KEY

    companion object {
        val KEY: StubIndexKey<String, FuncFile> = StubIndexKey.createIndexKey(FuncFileIndex::class.java.canonicalName)
    }
}

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

class FuncIncludeIndex : StringStubIndexExtension<FuncIncludePath>() {
    override fun getVersion(): Int = FuncFileStub.Type.stubVersion
    override fun getKey(): StubIndexKey<String, FuncIncludePath> = KEY

    companion object {
        val KEY: StubIndexKey<String, FuncIncludePath> =
            StubIndexKey.createIndexKey(FuncIncludePath::class.java.canonicalName)
    }
}