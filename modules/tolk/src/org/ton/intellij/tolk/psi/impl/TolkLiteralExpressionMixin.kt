package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkLiteralExpression
import org.ton.intellij.tolk.type.TolkType

abstract class TolkLiteralExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkLiteralExpression {
    override val type: TolkType?
        get() = when {
            integerLiteral != null -> TolkType.Int
            trueKeyword != null -> TolkType.Int
            falseKeyword != null -> TolkType.Int
            else -> {
                val stringLiteral = stringLiteral?.closingQuote?.text
                when (stringLiteral) {
                    null -> null
                    "\"u", "\"h", "\'H", "\"c" -> TolkType.Int
                    else -> TolkType.Slice
                }
            }
        }
}