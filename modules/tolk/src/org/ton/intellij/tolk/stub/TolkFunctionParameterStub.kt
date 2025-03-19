package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkParameter

class TolkParameterStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    val isMutable: Boolean,
) : TolkNamedStub<TolkParameter>(parent, elementType, name) {
    constructor(
        parent: StubElement<*>,
        elementType: IStubElementType<*, *>,
        name: String?,
        mutable: Boolean,
    ) : this(parent, elementType, StringRef.fromString(name), mutable)

    override fun toString(): String = buildString {
        append("TolkParameterStub(")
        if (isMutable) {
            append("mutable=true, ")
        }
        append("name=").append(name)
        append(")")
    }
}
