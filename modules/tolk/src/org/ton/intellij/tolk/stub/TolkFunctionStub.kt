package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.ton.intellij.tolk.psi.TolkFunction

class TolkFunctionStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: String?,
    val isMutable: Boolean,
    val isGetMethod: Boolean,
    val hasAsm: Boolean,
    val isBuiltin: Boolean,
    val isDeprecated: Boolean,
    val hasSelf: Boolean,
) : TolkNamedStub<TolkFunction>(parent, elementType, name) {
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
