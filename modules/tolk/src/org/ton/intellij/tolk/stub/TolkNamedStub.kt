package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkNamedElement

abstract class TolkNamedStub<T : TolkNamedElement>(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    val isDeprecated: Boolean,
) : NamedStubBase<T>(
    parent, elementType, name
) {
    constructor(parent: StubElement<*>, elementType: IStubElementType<*, *>, name: String?, isDeprecated: Boolean) : this(
        parent,
        elementType,
        StringRef.fromString(name),
        isDeprecated,
    )
}
