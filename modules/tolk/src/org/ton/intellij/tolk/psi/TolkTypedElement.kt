package org.ton.intellij.tolk.psi

import org.ton.intellij.tolk.type.TolkType
import org.ton.intellij.tolk.type.TolkTypeProvider
import org.ton.intellij.tolk.type.infer.inference

interface TolkTypedElement : TolkElement, TolkTypeProvider {
    override val type: TolkType?
        get() = if (this is TolkExpression) inference?.getType(this) else null
}