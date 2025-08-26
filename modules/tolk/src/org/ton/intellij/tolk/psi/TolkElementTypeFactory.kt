package org.ton.intellij.tolk.psi

import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.impl.*
import org.ton.intellij.tolk.stub.*
import org.ton.intellij.tolk.stub.type.*

class TolkTypeExpressionImpl2 : TolkTypeExpressionImpl {
    constructor(stub: TolkTypeStub<*>, stubType: IStubElementType<*, *>) : super(stub, stubType)
}

object TolkElementTypeFactory {
    @JvmStatic
    fun stubFactory(name: String): IStubElementType<*, *> = when (name) {
        "INCLUDE_DEFINITION"        -> TolkIncludeDefinitionStubElementType(name)
        "FUNCTION"                  -> TolkFunctionStubElementType(name)
        "PARAMETER_LIST"            -> TolkParameterListStub.Type(name)
        "PARAMETER"                 -> TolkParameterStubElementType(name)
        "SELF_PARAMETER"            -> TolkSelfParameterStub.Type
        "GLOBAL_VAR"                -> TolkGlobalVarStub.Type
        "CONST_VAR"                 -> TolkConstVarStub.Type
        "TYPE_PARAMETER"            -> TolkTypeParameterStubElementType(name)
        "TYPE_DEF"                  -> TolkTypeDefStub.Type
        "STRUCT"                    -> TolkStructStub.Type
        "STRUCT_FIELD"              -> TolkStructFieldStubElementType(name)
        "RETURN_TYPE"               -> TolkPlaceholderStub.Type(name, ::TolkReturnTypeImpl)
        "SELF_RETURN_TYPE"          -> TolkSelfReturnTypeStub.Type(name)
        "FUNCTION_RECEIVER"         -> TolkFunctionReceiverStub.Type(name)
        "TYPE_EXPRESSION"           -> TolkTypeStub.Type(name, ::TolkTypeExpressionImpl2)
        "FUN_TYPE_EXPRESSION"       -> TolkTypeStub.Type(name, ::TolkFunTypeExpressionImpl)
        "UNION_TYPE_EXPRESSION"     -> TolkTypeStub.Type(name, ::TolkUnionTypeExpressionImpl)
        "NULLABLE_TYPE_EXPRESSION"  -> TolkTypeStub.Type(name, ::TolkNullableTypeExpressionImpl)
        "NULL_TYPE_EXPRESSION"      -> TolkTypeStub.Type(name, ::TolkNullTypeExpressionImpl)
        "SELF_TYPE_EXPRESSION"      -> TolkTypeStub.Type(name, ::TolkSelfTypeExpressionImpl)
        "AUTO_TYPE_EXPRESSION"      -> TolkTypeStub.Type(name, ::TolkAutoTypeExpressionImpl)
        "TENSOR_TYPE_EXPRESSION"    -> TolkTypeStub.Type(name, ::TolkTensorTypeExpressionImpl)
        "PAREN_TYPE_EXPRESSION"     -> TolkTypeStub.Type(name, ::TolkParenTypeExpressionImpl)
        "TUPLE_TYPE_EXPRESSION"     -> TolkTypeStub.Type(name, ::TolkTupleTypeExpressionImpl)
        "REFERENCE_TYPE_EXPRESSION" -> TolkTypeStub.Type(name, ::TolkReferenceTypeExpressionImpl)
        "TYPE_ARGUMENT_LIST"        -> TolkTypeArgumentListStub.Type(name)
        "TYPE_PARAMETER_LIST"       -> TolkTypeParameterListStub.Type(name)
        "STRING_LITERAL"            -> TolkStringLiteralStub.Type(name)
        else                        -> throw RuntimeException("Unknown element type: $name")
    }
}
