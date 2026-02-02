package org.ton.intellij.tolk.eval

import org.ton.intellij.tolk.psi.TolkLiteralExpression
import java.math.BigInteger

sealed interface TolkValue

object TolkUnknownValue : TolkValue

data class TolkIntValue(val value: BigInteger) : TolkValue {
    override fun toString(): String = value.toString()

}

data class TolkTupleValue(val values: List<TolkValue?>) : TolkValue

data class TolkTensorValue(val values: List<TolkValue?>) : TolkValue

data class TolkStringValue(val value: String) : TolkValue

@OptIn(ExperimentalUnsignedTypes::class)
val TolkLiteralExpression.value: TolkValue
    get() {
        if (trueKeyword != null) {
            return TolkIntValue(-BigInteger.ONE)
        }
        if (falseKeyword != null) {
            return TolkIntValue(BigInteger.ZERO)
        }
        val integerLiteral = integerLiteral
        if (integerLiteral != null) {
            var text = integerLiteral.text.replace("_", "")
            val isNegative = text.startsWith("-")
            if (isNegative) {
                text = text.substring(1)
            }
            var integer = try {
                if (text.startsWith("0x") || text.startsWith("0X")) {
                    BigInteger(text.substring(2), 16)
                } else if (text.startsWith("0b") || text.startsWith("0B")) {
                    BigInteger(text.substring(2), 2)
                } else {
                    BigInteger(text)
                }
            } catch (e: NumberFormatException) {
                return TolkIntValue(BigInteger.ZERO)
            }
            if (isNegative) {
                integer = integer.negate()
            }
            return TolkIntValue(integer)
        }
        val stringLiteral = stringLiteral
        if (stringLiteral != null) {
            val text = stringLiteral.text
            if (text.startsWith("\"\"\"") && text.endsWith("\"\"\"")) {
                return TolkStringValue(text.removeSurrounding("\"\"\""))
            }
            return TolkStringValue(text.removeSurrounding("\""))
        }
        return TolkIntValue(BigInteger.ZERO)
    }
