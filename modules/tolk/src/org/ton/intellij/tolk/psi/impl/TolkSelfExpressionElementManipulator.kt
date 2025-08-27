package org.ton.intellij.tolk.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import org.ton.intellij.tolk.psi.TolkSelfExpression

class TolkSelfExpressionElementManipulator : AbstractElementManipulator<TolkSelfExpression>() {
    override fun handleContentChange(
        element: TolkSelfExpression,
        range: TextRange,
        newContent: String,
    ): TolkSelfExpression {
        return element
    }
}
