package org.ton.intellij.tolk.psi

import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTypeProvider
import org.ton.intellij.tolk.type.inference

interface TolkTypedElement : TolkElement, TolkTypeProvider {
    override val type: TolkTy?
        get() = if (this is TolkExpression) inference?.getType(this) else null
}
