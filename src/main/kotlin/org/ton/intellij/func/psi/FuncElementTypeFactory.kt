package org.ton.intellij.func.psi

import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.func.stub.type.*

object FuncElementTypeFactory {
    @JvmStatic
    fun stubFactory(name: String): IStubElementType<*, *> = when (name) {
        "INCLUDE_DEFINITION" -> FuncIncludeDefinitionStubElementType(name)
        "FUNCTION" -> FuncFunctionStubElementType(name)
        "FUNCTION_PARAMETER" -> FuncFunctionParameterStubElementType(name)
        "GLOBAL_VAR" -> FuncGlobalVarStubElementType(name)
        "TYPE_PARAMETER" -> FuncTypeParameterStubElementType(name)
        else -> throw RuntimeException("Unknown element type: $name")
    }
}
