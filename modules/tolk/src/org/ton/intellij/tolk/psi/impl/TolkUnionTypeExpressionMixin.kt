package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkUnionTypeExpression
import org.ton.intellij.tolk.type.TolkType

abstract class TolkUnionTypeExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkUnionTypeExpression {
    override val type: TolkType
        get() {
            val types = typeExpressionList.map { it.type ?: TolkType.Unknown  }
            val unionTypes = TolkType.union(types)
            return unionTypes
        }
}
