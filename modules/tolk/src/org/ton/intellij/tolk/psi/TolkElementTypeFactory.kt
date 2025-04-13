package org.ton.intellij.tolk.psi

import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.stub.type.*

object TolkElementTypeFactory {
    @JvmStatic
    fun stubFactory(name: String): IStubElementType<*, *> = when (name) {
        "INCLUDE_DEFINITION" -> TolkIncludeDefinitionStubElementType(name)
        "FUNCTION" -> TolkFunctionStubElementType(name)
        "PARAMETER" -> TolkParameterStubElementType(name)
        "GLOBAL_VAR" -> TolkGlobalVarStubElementType(name)
        "CONST_VAR" -> TolkConstVarStubElementType(name)
        "TYPE_PARAMETER" -> TolkTypeParameterStubElementType(name)
        "TYPE_DEF" -> TolkTypeDefStubElementType(name)
        "STRUCT" -> TolkStructStubElementType(name)
        "STRUCT_FIELD" -> TolkStructFieldStubElementType(name)
        else -> throw RuntimeException("Unknown element type: $name")
    }
}
