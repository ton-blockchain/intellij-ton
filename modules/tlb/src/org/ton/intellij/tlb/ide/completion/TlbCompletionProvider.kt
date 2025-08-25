package org.ton.intellij.tlb.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement

abstract class TlbCompletionProvider : CompletionProvider<CompletionParameters>() {
    abstract val elementPattern: ElementPattern<out PsiElement>
}
