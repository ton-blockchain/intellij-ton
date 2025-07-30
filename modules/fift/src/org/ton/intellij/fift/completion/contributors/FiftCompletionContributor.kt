package org.ton.intellij.fift.completion.contributors

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import org.ton.intellij.fift.completion.FiftAsmInstructionsCompletionProvider
import org.ton.intellij.fift.psi.FiftTvmInstruction

class FiftCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, onAsmInstruction(), FiftAsmInstructionsCompletionProvider)
    }

    private fun onAsmInstruction(): PsiElementPattern.Capture<PsiElement> = psiElement()
        .withSuperParent(1, FiftTvmInstruction::class.java)
}
