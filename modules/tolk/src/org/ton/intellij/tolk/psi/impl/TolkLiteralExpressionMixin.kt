package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.apache.commons.codec.binary.Hex
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkLiteralExpression
import org.ton.intellij.tolk.type.TolkConstantIntType
import org.ton.intellij.tolk.type.TolkType
import java.math.BigInteger
import java.security.MessageDigest
import java.util.zip.CRC32

abstract class TolkLiteralExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkLiteralExpression {
    override val type: TolkType?
        get() {
            val treeParent = node.treeParent
            if (treeParent.elementType == TolkElementTypes.DOT_EXPRESSION && treeParent.lastChildNode == node) {
                return super.type
            }
            val integerLiteral = integerLiteral?.text
            if (integerLiteral != null) {
                var text = integerLiteral.replace("_", "")
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
                    return TolkType.Int
                }
                if (isNegative) {
                    integer = integer.negate()
                }
                return TolkConstantIntType(integer)
            }
            if (trueKeyword != null) {
                return TolkType.TRUE
            }
            if (falseKeyword != null) {
                return TolkType.FALSE
            }
            if (nullKeyword != null) {
                return TolkType.Null
            }
            val stringLiteral = stringLiteral
            if (stringLiteral != null) {
                val text = stringLiteral.text
                if (text.length < 2) return TolkType.Slice
                val tag = text.lastOrNull()
                if (tag == '"') {
                    return TolkType.Slice
                }
                when (tag) {
                    'u' -> {
                        if (text.length <= 3) return TolkConstantIntType(BigInteger.ZERO)
                        val rawValue = text.substring(1, text.length - 2)
                        val intValue = BigInteger(Hex.encodeHexString(rawValue.encodeToByteArray()), 16)
                        return TolkConstantIntType(intValue)
                    }

                    'h' -> {
                        if (text.length <= 3) return TolkConstantIntType(BigInteger.ZERO)
                        val rawValue = text.substring(1, text.length - 2)
                        val digestValue = MessageDigest.getInstance("SHA-256").apply {
                            update(rawValue.toByteArray())
                        }.digest()
                        val intValue = BigInteger(Hex.encodeHexString(digestValue).substring(0, 8), 16)
                        return TolkConstantIntType(intValue)
                    }

                    'H' -> {
                        if (text.length <= 3) return TolkConstantIntType(BigInteger.ZERO)
                        val rawValue = text.substring(1, text.length - 2)
                        val digestValue = MessageDigest.getInstance("SHA-256").apply {
                            update(rawValue.toByteArray())
                        }.digest()
                        val intValue = BigInteger(Hex.encodeHexString(digestValue), 16)
                        return TolkConstantIntType(intValue)
                    }

                    'c' -> {
                        if (text.length <= 3) return TolkConstantIntType(BigInteger.ZERO)
                        val rawValue = text.substring(1, text.length - 2)
                        val intValue = CRC32().apply {
                            update(rawValue.toByteArray())
                        }.value
                        return TolkConstantIntType(BigInteger.valueOf(intValue))
                    }

                    else -> {
                        return TolkType.Unknown
                    }
                }
            }
            return TolkType.Unknown
        }

    override fun toString(): String = "TolkLiteralExpression($type)"
}
