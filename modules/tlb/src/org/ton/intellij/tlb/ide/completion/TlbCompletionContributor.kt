package org.ton.intellij.tlb.ide.completion

import com.intellij.codeInsight.completion.*
import org.ton.intellij.tlb.ide.completion.providers.TolkBuiltinTypesCompletionProvider
import org.ton.intellij.tlb.ide.completion.providers.TolkTypesCompletionProvider

class TlbCompletionContributor : CompletionContributor() {
    init {
        extend(TolkBuiltinTypesCompletionProvider)
        extend(TolkTypesCompletionProvider)
    }

    fun extend(provider: TlbCompletionProvider) {
        extend(CompletionType.BASIC, provider.elementPattern, provider)
    }
}
