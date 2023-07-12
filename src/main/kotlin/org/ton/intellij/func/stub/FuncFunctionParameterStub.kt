package org.ton.intellij.func.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.ton.intellij.func.psi.FuncFunctionParameter

class FuncFunctionParameterStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
) : FuncNamedStub<FuncFunctionParameter>(parent, elementType, name) {
    constructor(
        parent: StubElement<*>,
        elementType: IStubElementType<*, *>,
        name: String?,
    ) : this(parent, elementType, StringRef.fromString(name))

    override fun toString(): String = buildString {
        append("FuncFunctionParameterStub(")
        append("name=").append(name)
        append(")")
    }
}
