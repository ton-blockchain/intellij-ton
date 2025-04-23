package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkFunction

class TolkFunctionStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    val isMutable: Boolean,
    val isGetMethod: Boolean,
    val hasAsm: Boolean,
    val isBuiltin: Boolean,
    val isDeprecated: Boolean,
    val isGeneric: Boolean,
    val hasSelf: Boolean
) : TolkNamedStub<TolkFunction>(parent, elementType, name) {
    constructor(
        parent: StubElement<*>, elementType: IStubElementType<*, *>,
        name: String?,
        isMutable: Boolean,
        isGetMethod: Boolean,
        hasAsm: Boolean,
        isBuiltin: Boolean,
        isDeprecated: Boolean,
        isGeneric: Boolean,
        hasSelf: Boolean
    ) : this(
        parent,
        elementType,
        StringRef.fromString(name),
        isMutable,
        isGetMethod,
        hasAsm,
        isBuiltin,
        isDeprecated,
        isGeneric,
        hasSelf
    )

    override fun toString(): String = buildString {
        append("TolkFunctionStub(")
        append("name=").append(name).append(", ")
        append("isMutable=").append(isMutable).append(", ")
        append("isGetMethod=").append(isGetMethod).append(", ")
        append("hasAsm=").append(hasAsm)
        append("isBuiltin=").append(isBuiltin)
        append("isDeprecated=").append(isDeprecated)
        append("hasSelf=").append(hasSelf)
        append(")")
    }
}
