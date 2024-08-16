package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkFunctionParameter

class TolkFunctionParameterStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
) : TolkNamedStub<TolkFunctionParameter>(parent, elementType, name) {
    constructor(
        parent: StubElement<*>,
        elementType: IStubElementType<*, *>,
        name: String?,
    ) : this(parent, elementType, StringRef.fromString(name))

    override fun toString(): String = buildString {
        append("TolkFunctionParameterStub(")
        append("name=").append(name)
        append(")")
    }
}
