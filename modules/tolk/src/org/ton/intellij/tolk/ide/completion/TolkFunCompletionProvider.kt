package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.util.prevVisibleOrNewLine
import org.ton.intellij.util.psiElement

object TolkFunCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement<PsiElement>()
            .withParent(psiElement<TolkFunction>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        if (position.prevVisibleOrNewLine.elementType == TolkElementTypes.GET_KEYWORD) {
            result.addElement(
                LookupElementBuilder.create("fun").withBoldness(true)
            )
        }
    }
}