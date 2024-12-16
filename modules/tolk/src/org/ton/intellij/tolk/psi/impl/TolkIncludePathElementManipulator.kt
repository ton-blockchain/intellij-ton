package org.ton.intellij.tolk.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import org.ton.intellij.tolk.psi.TolkIncludeDefinition
import org.ton.intellij.tolk.psi.TolkPsiFactory

class TolkIncludePathElementManipulator : AbstractElementManipulator<TolkIncludeDefinition>() {
    override fun handleContentChange(
        element: TolkIncludeDefinition,
        range: TextRange,
        newContent: String,
    ): TolkIncludeDefinition {
        val newText = range.replace(element.text, newContent)
        val newStringLiteral =
            TolkPsiFactory[element.project].createFile(newText).includeDefinitions.first().stringLiteral
        if (newStringLiteral != null) {
            element.stringLiteral?.replace(newStringLiteral)
        }
        return element
    }
}
