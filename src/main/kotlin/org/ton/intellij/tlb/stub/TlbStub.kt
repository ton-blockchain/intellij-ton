package org.ton.intellij.tlb.stub

fun factory(name: String): TlbStubElementType<*, *> {
    return when (name) {
        "CONSTRUCTOR" -> TlbConstructorStub.Type
        else -> error("Unknown element type: $name")
    }
}
