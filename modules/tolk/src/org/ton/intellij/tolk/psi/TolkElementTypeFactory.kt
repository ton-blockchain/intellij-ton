package org.ton.intellij.tolk.psi

import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.impl.TolkReturnTypeImpl
import org.ton.intellij.tolk.stub.TolkPlaceholderStub
import org.ton.intellij.tolk.stub.TolkSelfParameterStub
import org.ton.intellij.tolk.stub.type.*

object TolkElementTypeFactory {
    @JvmStatic
    fun stubFactory(name: String): IStubElementType<*, *> = when (name) {
        "INCLUDE_DEFINITION" -> TolkIncludeDefinitionStubElementType(name)
        "FUNCTION" -> TolkFunctionStubElementType(name)
        "PARAMETER" -> TolkParameterStubElementType(name)
        "SELF_PARAMETER" -> TolkSelfParameterStub.Type
        "GLOBAL_VAR" -> TolkGlobalVarStubElementType(name)
        "CONST_VAR" -> TolkConstVarStubElementType(name)
        "TYPE_PARAMETER" -> TolkTypeParameterStubElementType(name)
        "TYPE_DEF" -> TolkTypeDefStubElementType(name)
        "STRUCT" -> TolkStructStubElementType(name)
        "STRUCT_FIELD" -> TolkStructFieldStubElementType(name)
        "RETURN_TYPE" -> TolkPlaceholderStub.Type(name, ::TolkReturnTypeImpl)
        else -> throw RuntimeException("Unknown element type: $name")
    }
}
