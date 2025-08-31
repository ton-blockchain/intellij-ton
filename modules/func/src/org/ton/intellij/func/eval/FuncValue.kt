package org.ton.intellij.func.eval

import org.apache.commons.codec.binary.Hex
import org.ton.intellij.func.psi.FuncLiteralExpression
import org.ton.intellij.func.type.ty.*
import java.math.BigInteger
import java.security.MessageDigest
import java.util.zip.CRC32

sealed interface FuncValue : FuncTyProvider

object FuncUnknownValue : FuncValue {
    override fun getFuncTy(): FuncTy {
        return FuncTyUnknown
    }
}

data class FuncIntValue(val value: BigInteger) : FuncValue {
    override fun toString(): String = value.toString()
    override fun getFuncTy(): FuncTy {
        return FuncTyInt
    }
}

data class FuncTupleValue(val values: List<FuncValue?>) : FuncValue {
    override fun getFuncTy(): FuncTy {
        return FuncTyTuple(values.map { it?.getFuncTy() ?: FuncTyUnknown })
    }
}

data class FuncTensorValue(val values: List<FuncValue?>) : FuncValue {
    override fun getFuncTy(): FuncTy {
        return FuncTyTensor(values.map { it?.getFuncTy() ?: FuncTyUnknown })
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
val FuncLiteralExpression.value: FuncValue
    get() {
        if (trueKeyword != null) {
            return FuncIntValue(-BigInteger.ONE)
        }
        if (falseKeyword != null) {
            return FuncIntValue(BigInteger.ZERO)
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
                return FuncIntValue(BigInteger.ZERO)
            }
            if (isNegative) {
                integer = integer.negate()
            }
            return FuncIntValue(integer)
        }
        val stringLiteral = stringLiteral
        if (stringLiteral != null) {
            val text = stringLiteral.text
            if (text.length < 2) return FuncIntValue(BigInteger.ZERO)
            val tag = text.lastOrNull()
            if (tag == '"') {
                return FuncIntValue(BigInteger.ZERO) // TODO: Slice type
            }
            when (tag) {
                'u' -> {
                    if (text.length <= 3) return FuncIntValue(BigInteger.ZERO)
                    val rawValue = text.substring(1, text.length - 2)
                    val intValue = BigInteger(Hex.encodeHexString(rawValue.encodeToByteArray()), 16)
                    return FuncIntValue(intValue)
                }

                'h' -> {
                    if (text.length <= 3) return FuncIntValue(BigInteger.ZERO)
                    val rawValue = text.substring(1, text.length - 2)
                    val digestValue = MessageDigest.getInstance("SHA-256").apply {
                        update(rawValue.toByteArray())
                    }.digest()
                    val intValue = BigInteger(Hex.encodeHexString(digestValue).substring(0, 8), 16)
                    return FuncIntValue(intValue)
                }

                'H' -> {
                    if (text.length <= 3) return FuncIntValue(BigInteger.ZERO)
                    val rawValue = text.substring(1, text.length - 2)
                    val digestValue = MessageDigest.getInstance("SHA-256").apply {
                        update(rawValue.toByteArray())
                    }.digest()
                    val intValue = BigInteger(Hex.encodeHexString(digestValue), 16)
                    return FuncIntValue(intValue)
                }

                'c' -> {
                    if (text.length <= 3) return FuncIntValue(BigInteger.ZERO)
                    val rawValue = text.substring(1, text.length - 2)
                    val intValue = CRC32().apply {
                        update(rawValue.toByteArray())
                    }.value
                    return FuncIntValue(BigInteger.valueOf(intValue))
                }

                else -> {
                    return FuncIntValue(BigInteger.ZERO)
                }
            }
        }
        return FuncIntValue(BigInteger.ZERO)
    }

val FuncLiteralExpression.isIntegerString: Boolean
    get() {
        val stringLiteral = stringLiteral ?: return false
        val text = stringLiteral.text
        if (text.length < 2) return false
        val tag = text.lastOrNull()
        if (tag == '"') {
            return false
        }
        return when (tag) {
            'u', 'h', 'H', 'c' -> true
            else               -> false
        }
    }
