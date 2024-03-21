package org.ton.intellij.func.ide.completion

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import org.ton.intellij.func.psi.FuncNamedElement

class FuncCompletionConfidence : CompletionConfidence() {
    override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        return if (contextElement is FuncNamedElement && contextElement.name == "_") {
            ThreeState.YES
        } else ThreeState.UNSURE
    }
}
