package org.ton.intellij.func.psi

import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.func.stub.type.FuncIncludeDefinitionStubElementType

object FuncElementTypeFactory {
    @JvmStatic
    fun stubFactory(name: String): IStubElementType<*, *> = when (name) {
        "INCLUDE_DEFINITION" -> FuncIncludeDefinitionStubElementType(name)
        else -> throw RuntimeException("Unknown element type: $name")
    }
}
