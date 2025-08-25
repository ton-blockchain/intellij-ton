package org.ton.intellij.tlb.ide.completion

import com.intellij.codeInsight.completion.*
import org.ton.intellij.tlb.ide.completion.providers.TolkBuiltinTypesCompletionProvider

class TlbCompletionContributor : CompletionContributor() {
    init {
        extend(TolkBuiltinTypesCompletionProvider)
    }

    fun extend(provider: TlbCompletionProvider) {
        extend(CompletionType.BASIC, provider.elementPattern, provider)
    }
}
