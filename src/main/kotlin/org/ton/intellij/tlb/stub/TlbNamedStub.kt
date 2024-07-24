package org.ton.intellij.tlb.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.ton.intellij.tlb.psi.TlbNamedElement

abstract class TlbNamedStub<T : TlbNamedElement>(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    name: StringRef?
) : NamedStubBase<T>(parent, elementType, name) {
    constructor(parent: StubElement<*>, elementType: IStubElementType<*, *>, name: String?) : this(
        parent,
        elementType,
        StringRef.fromString(name)
    )

    override fun toString(): String {
        return "${javaClass.simpleName}($name)"
    }
}
