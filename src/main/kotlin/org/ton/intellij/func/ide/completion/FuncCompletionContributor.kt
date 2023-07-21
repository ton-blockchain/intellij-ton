package org.ton.intellij.func.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement
import org.ton.intellij.func.psi.FuncReferenceExpression

class FuncCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, referenceExpression(), FuncReferenceCompletionProvider())
    }

    private fun referenceExpression() =
        psiElement().withParent(FuncReferenceExpression::class.java)
}
