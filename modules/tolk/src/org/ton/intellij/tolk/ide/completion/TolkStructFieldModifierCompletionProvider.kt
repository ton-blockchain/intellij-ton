package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkStructBody
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.util.prevVisibleOrNewLine

object TolkStructFieldModifierCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> = psiElement()
        .inside(TolkStructBody::class.java)
        .andNot(psiElement().withParent(TolkStructField::class.java).afterLeaf(":"))
        .and(psiElement().with(object : PatternCondition<PsiElement>("notAfterColon") {
            override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
                return t.prevVisibleOrNewLine?.text != ":"
            }
        }))

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val provider = TolkKeywordCompletionProvider(
            TolkCompletionContributor.CONTEXT_KEYWORD_PRIORITY,
            listOf("private", "readonly")
        )
        provider.addCompletions(parameters, context, result)
    }
}
