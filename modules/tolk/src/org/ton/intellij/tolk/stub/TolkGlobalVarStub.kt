package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkGlobalVar

class TolkGlobalVarStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
) : TolkNamedStub<TolkGlobalVar>(parent, elementType, name) {
    constructor(
        parent: StubElement<*>, elementType: IStubElementType<*, *>,
        name: String?,
    ) : this(
        parent,
        elementType,
        StringRef.fromString(name),
    )
}
