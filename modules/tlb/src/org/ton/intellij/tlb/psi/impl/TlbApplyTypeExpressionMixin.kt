package org.ton.intellij.tlb.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tlb.TlbSize
import org.ton.intellij.tlb.psi.TlbApplyTypeExpression
import org.ton.intellij.tlb.psi.TlbParamTypeExpression
import org.ton.intellij.tlb.psi.naturalValue
import org.ton.intellij.tlb.psi.unwrap
import kotlin.math.min

abstract class TlbApplyTypeExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TlbApplyTypeExpression {
    override val tlbSize: TlbSize?
        get() {
            val args = argumentList.typeExpressionList
            val type = typeExpression.unwrap()
            if (args.size == 1 && type is TlbParamTypeExpression) {
                val value = args.first().naturalValue()
                if (value != null) {
                    val size = when(type.text) {
                        "##", "int", "uint", "bits" ->  TlbSize.fixedSize(min(value, 2047))
                        "#<=" ->  TlbSize.fixedSize(32 - value.countLeadingZeroBits())
                        "#<" ->  TlbSize.fixedSize(
                            if (value != 0) 32 - (value - 1).countLeadingZeroBits() else 2047
                        )
                        else -> null
                    }
                    if (size != null) {
                        return size
                    }
                }
            }
            // resolve size for types
            return null
        }
}