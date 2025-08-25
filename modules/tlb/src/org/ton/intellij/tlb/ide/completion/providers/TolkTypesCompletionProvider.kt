package org.ton.intellij.tlb.ide.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tlb.TlbIcons
import org.ton.intellij.tlb.ide.completion.TlbCompletionProvider
import org.ton.intellij.tlb.psi.TlbFile
import org.ton.intellij.tlb.psi.TlbTypeExpression

object TolkTypesCompletionProvider : TlbCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        PlatformPatterns.psiElement()
            .withParent(TlbTypeExpression::class.java)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val file = parameters.originalFile as? TlbFile ?: return
        file.resultTypes().forEach { type ->
            result.addElement(
                LookupElementBuilder.create(type)
                    .withIcon(TlbIcons.TYPE)
            )
        }
    }
}
