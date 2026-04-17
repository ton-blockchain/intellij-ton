package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import org.ton.intellij.tolk.psi.TolkNamedElement

class TolkCompletionConfidence : CompletionConfidence() {
    override fun shouldSkipAutopopup(
        editor: Editor,
        contextElement: PsiElement,
        psiFile: PsiFile,
        offset: Int,
    ): ThreeState = if (contextElement is TolkNamedElement && contextElement.name == "_") {
        ThreeState.YES
    } else {
        ThreeState.UNSURE
    }
}
