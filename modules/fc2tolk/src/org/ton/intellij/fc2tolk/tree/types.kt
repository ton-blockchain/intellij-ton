package org.ton.intellij.fc2tolk.tree

import org.ton.intellij.fc2tolk.symbols.FTTypeParameterSymbol

interface FTType

object FTUnknownType : FTType

enum class FTFuncPrimitiveType(val value: String) : FTType {
    INT("int"),
    CELL("cell"),
    BUILDER("builder"),
    SLICE("slice"),
    TUPLE("tuple"),
    CONT("cont"),
}

object FTFuncHoleType : FTType

class FTTensorType private constructor(val value: List<FTType>) : FTType {
    companion object {
        fun create(value: List<FTType>): FTType = if (value.size == 1) value[0] else FTTensorType(value)
    }
}

class FTTupleType(val value: List<FTType>) : FTType

class FTTypeParameterType(
    val identifier: FTTypeParameterSymbol
) : FTType