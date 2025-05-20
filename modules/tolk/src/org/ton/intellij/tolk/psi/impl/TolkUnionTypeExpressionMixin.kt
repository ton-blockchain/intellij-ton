package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkUnionTypeExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkUnionTypeExpressionMixin(node: ASTNode) : TolkTypeExpressionImpl(node), TolkUnionTypeExpression {
    override val type: TolkTy
        get() {
            val types = typeExpressionList.map { it.type ?: TolkTy.Unknown  }
            val unionTypes = TolkTy.union(types)
            return unionTypes
        }
}
