package org.ton.intellij.func.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import org.ton.intellij.func.psi.FuncIncludeDefinition
import org.ton.intellij.func.psi.FuncPsiFactory

class FuncIncludePathElementManipulator : AbstractElementManipulator<FuncIncludeDefinition>() {
    override fun handleContentChange(
        element: FuncIncludeDefinition,
        range: TextRange,
        newContent: String,
    ): FuncIncludeDefinition {
        val newText = range.replace(element.text, newContent)
        val newStringLiteral =
            FuncPsiFactory[element.project].createFileFromText(newText).includeDefinitions.first().stringLiteral
        element.stringLiteral.replace(newStringLiteral)
        return element
    }
}
