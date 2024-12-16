package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkIncludeDefinition

class TolkIncludeDefinitionStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*,*>,
    path: StringRef
) : StubBase<TolkIncludeDefinition>(
    parent, elementType
) {
    constructor(parent: StubElement<*>, elementType: IStubElementType<*, *>, path: String) : this(parent, elementType, StringRef.fromString(path))

    private val _path: StringRef = path

    val path: String get() = _path.string

    override fun toString(): String = "TolkIncludeDefinitionStub(path='$path')"
}
