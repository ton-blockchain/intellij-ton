package org.ton.intellij.func.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class FuncCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, FuncCommonCompletionProvider)
    }

    fun extend(type: CompletionType?, provider: FuncCompletionProvider) {
        extend(type, provider.elementPattern, provider)
    }

    companion object {
        const val KEYWORD_PRIORITY = 20.0
        const val CONTEXT_KEYWORD_PRIORITY = 25.0
        const val NOT_IMPORTED_FUNCTION_PRIORITY = 3.0
        const val FUNCTION_PRIORITY = NOT_IMPORTED_FUNCTION_PRIORITY + 10.0
        const val NOT_IMPORTED_VAR_PRIORITY = 5.0
        const val VAR_PRIORITY = NOT_IMPORTED_VAR_PRIORITY + 10.0
    }
}
