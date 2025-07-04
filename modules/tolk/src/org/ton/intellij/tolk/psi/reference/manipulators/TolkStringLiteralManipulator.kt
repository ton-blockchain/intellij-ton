package org.ton.intellij.tolk.psi.reference.manipulators

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import org.ton.intellij.tolk.psi.TolkStringLiteral
import org.ton.intellij.tolk.psi.tolkPsiFactory

class TolkStringLiteralManipulator : AbstractElementManipulator<TolkStringLiteral>() {
    override fun handleContentChange(
        element: TolkStringLiteral,
        range: TextRange,
        newContent: String
    ): TolkStringLiteral {
        val newStr = element.project.tolkPsiFactory.createStringLiteral(newContent)
        return element.replace(newStr) as TolkStringLiteral
    }
}
