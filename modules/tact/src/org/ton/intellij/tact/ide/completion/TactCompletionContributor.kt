package org.ton.intellij.tact.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class TactCompletionContributor : CompletionContributor() {
    init {
        extend(TactTypeCompletionProvider())
        extend(TactReferenceCompletionProvider())
        extend(TactDotCompletionProvider())
    }

    fun extend(provider: TactCompletionProvider) {
        extend(CompletionType.BASIC, provider.elementPattern, provider)
    }
}
