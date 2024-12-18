package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkTypeIdentifier
import org.ton.intellij.tolk.psi.TolkTypeParameterListOwner
import org.ton.intellij.util.parentOfType
import org.ton.intellij.util.psiElement

object TolkTypeCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement<PsiElement>().withParent(psiElement<TolkTypeIdentifier>())

    private val primitiveTypes = listOf(
        "int", "slice", "cell", "builder", "tuple", "continuation", "bool"
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val parameterListOwner = position.parentOfType<TolkTypeParameterListOwner>()
        parameterListOwner?.typeParameterList?.typeParameterList?.forEach { type ->
            result.addElement(
                LookupElementBuilder.createWithIcon(type)
            )
        }
        primitiveTypes.forEach { type ->
            result.addElement(
                LookupElementBuilder.create(type).withBoldness(true)
            )
        }
    }
}