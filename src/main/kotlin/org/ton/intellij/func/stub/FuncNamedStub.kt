package org.ton.intellij.func.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.ton.intellij.func.psi.FuncNamedElement

class FuncNamedStub<T : FuncNamedElement>(
    parent: StubElement<*>,
    elementType: IStubElementType<*,*>,
    name: StringRef
) : NamedStubBase<T>(
    parent, elementType, name
) {
    constructor(parent: StubElement<*>, elementType: IStubElementType<*, *>, name: String) : this(parent, elementType, StringRef.fromString(name))
}
