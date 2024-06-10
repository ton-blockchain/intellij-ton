package org.ton.intellij.func.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.ton.intellij.func.psi.FuncFunction

class FuncFunctionStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    val isMutable: Boolean,
    val isImpure: Boolean,
    val hasMethodId: Boolean,
    val hasAsm: Boolean,
    val hasGet: Boolean,
    val hasPure: Boolean,
) : FuncNamedStub<FuncFunction>(parent, elementType, name) {
    constructor(
        parent: StubElement<*>, elementType: IStubElementType<*, *>,
        name: String?,
        isMutable: Boolean,
        isImpure: Boolean,
        hasMethodId: Boolean,
        hasAsm: Boolean,
        hasGet: Boolean,
        hasPure: Boolean
    ) : this(
        parent = parent,
        elementType = elementType,
        name = StringRef.fromString(name),
        isMutable = isMutable,
        isImpure = isImpure,
        hasMethodId = hasMethodId,
        hasAsm = hasAsm,
        hasGet = hasGet,
        hasPure = hasPure
    )

    override fun toString(): String = buildString {
        append("FuncFunctionStub(")
        append("name=").append(name).append(", ")
        append("isMutable=").append(isMutable).append(", ")
        append("isImpure=").append(isImpure).append(", ")
        append("hasMethodId=").append(hasMethodId).append(", ")
        append("hasAsm=").append(hasAsm).append(", ")
        append("hasGet=").append(hasGet).append(", ")
        append("hasPure=").append(hasPure)
        append(")")
    }
}
