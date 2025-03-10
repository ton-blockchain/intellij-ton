package org.ton.intellij.tolk.eval

import org.apache.commons.codec.binary.Hex
import org.ton.intellij.tolk.psi.TolkLiteralExpression
import java.math.BigInteger
import java.security.MessageDigest
import java.util.zip.CRC32

sealed interface TolkValue

object TolkUnknownValue : TolkValue

data class TolkIntValue(val value: BigInteger) : TolkValue {
    override fun toString(): String = value.toString()

}

data class TolkTupleValue(val values: List<TolkValue?>) : TolkValue

data class TolkTensorValue(val values: List<TolkValue?>) : TolkValue

data class TolkSliceValue(val value: String) : TolkValue

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
            if (text.length < 2) return TolkIntValue(BigInteger.ZERO)
            val tag = text.lastOrNull()
            if (tag == '"') {
                return TolkSliceValue(text.removeSurrounding("\""))
            }
            when (tag) {
                'u' -> {
                    if (text.length <= 3) return TolkIntValue(BigInteger.ZERO)
                    val rawValue = text.substring(1, text.length - 2)
                    val intValue = BigInteger(Hex.encodeHexString(rawValue.encodeToByteArray()), 16)
                    return TolkIntValue(intValue)
                }

                'h' -> {
                    if (text.length <= 3) return TolkIntValue(BigInteger.ZERO)
                    val rawValue = text.substring(1, text.length - 2)
                    val digestValue = MessageDigest.getInstance("SHA-256").apply {
                        update(rawValue.toByteArray())
                    }.digest()
                    val intValue = BigInteger(Hex.encodeHexString(digestValue).substring(0, 8), 16)
                    return TolkIntValue(intValue)
                }

                'H' -> {
                    if (text.length <= 3) return TolkIntValue(BigInteger.ZERO)
                    val rawValue = text.substring(1, text.length - 2)
                    val digestValue = MessageDigest.getInstance("SHA-256").apply {
                        update(rawValue.toByteArray())
                    }.digest()
                    val intValue = BigInteger(Hex.encodeHexString(digestValue), 16)
                    return TolkIntValue(intValue)
                }

                'c' -> {
                    if (text.length <= 3) return TolkIntValue(BigInteger.ZERO)
                    val rawValue = text.substring(1, text.length - 2)
                    val intValue = CRC32().apply {
                        update(rawValue.toByteArray())
                    }.value
                    return TolkIntValue(BigInteger.valueOf(intValue))
                }

                else -> {
                    return TolkIntValue(BigInteger.ZERO)
                }
            }
        }
        return TolkIntValue(BigInteger.ZERO)
    }
