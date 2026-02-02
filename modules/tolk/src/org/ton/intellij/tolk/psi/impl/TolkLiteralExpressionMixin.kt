package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.apache.commons.codec.binary.Hex
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkLiteralExpression
import org.ton.intellij.tolk.type.TolkConstantIntTy
import org.ton.intellij.tolk.type.TolkTy
import java.math.BigInteger
import java.security.MessageDigest
import java.util.zip.CRC32

abstract class TolkLiteralExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkLiteralExpression {
    override val type: TolkTy?
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
                    return TolkTy.Int
                }
                if (isNegative) {
                    integer = integer.negate()
                }
                return TolkConstantIntTy(integer)
            }
            if (trueKeyword != null) {
                return TolkTy.TRUE
            }
            if (falseKeyword != null) {
                return TolkTy.FALSE
            }
            if (nullKeyword != null) {
                return TolkTy.Null
            }
            if (stringLiteral != null) {
                return TolkTy.String
            }
            return TolkTy.Unknown
        }

    override fun toString(): String = "TolkLiteralExpression:$text"
}
