package org.ton.intellij.func.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.ton.intellij.func.psi.FuncIncludeDefinition

class FuncIncludeDefinitionStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    path: StringRef
) : StubBase<FuncIncludeDefinition>(
    parent, elementType
) {
    constructor(parent: StubElement<*>, elementType: IStubElementType<*, *>, path: String) : this(
        parent,
        elementType,
        StringRef.fromString(path)
    )

    private val _path: StringRef = path

    val path: String get() = _path.string

    override fun toString(): String = "FuncIncludeDefinitionStub(path='$path')"
}
