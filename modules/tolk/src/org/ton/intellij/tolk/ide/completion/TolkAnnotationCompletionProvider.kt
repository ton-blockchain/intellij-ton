package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

object TolkAnnotationCompletionProvider : TolkCompletionProvider(), DumbAware {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = psiElement().afterLeaf("@")

    private val lookupElements = listOf(
        LookupElementBuilder.create("pure"),
        LookupElementBuilder.create("inline"),
        LookupElementBuilder.create("inline_ref"),
        LookupElementBuilder.create("method_id"),
        LookupElementBuilder.create("deprecated"),
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        lookupElements.forEach {
            result.addElement(it)
        }
    }
}
