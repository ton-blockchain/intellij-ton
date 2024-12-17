package org.ton.intellij.tact.eval

import java.math.BigInteger

sealed interface TactValue

data class TactIntValue(
    val value: BigInteger
) : TactValue {
    override fun toString(): String = value.toString()
}

data class TactBoolValue(
    val value: Boolean
) : TactValue {
    override fun toString(): String = value.toString()
}

data class TactNullableValue(
    val value: TactValue?
) : TactValue {
    override fun toString(): String = value.toString()
}
