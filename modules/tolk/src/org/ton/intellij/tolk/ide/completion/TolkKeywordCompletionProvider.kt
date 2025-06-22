package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.ide.completion.TolkLookupElementData.KeywordKind.KEYWORD
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.impl.returnTy
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.util.addSuffix
import org.ton.intellij.util.ancestorStrict

class TolkKeywordCompletionProvider(
    val priority: Double,
    val keywords: List<String> = emptyList(),
) : CompletionProvider<CompletionParameters>() {
    constructor(priority: Double, vararg keywords: String) : this(priority, keywords.toList())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        keywords.asReversed().forEach{ s ->
            result.addElement(createKeywordLookupElement(s, parameters))
        }
    }

    private fun createKeywordLookupElement(
        keyword: String,
        parameters: CompletionParameters,
    ): LookupElement {
        var builder = LookupElementBuilder.create(keyword).bold()
        builder = addInsertionHandler(keyword, builder, parameters)
        return builder.toTolkLookupElement(
            TolkLookupElementData(keywordKind = KEYWORD)
        )
    }

    private fun addInsertionHandler(
        keyword: String, builder: LookupElementBuilder, parameters: CompletionParameters
    ): LookupElementBuilder {
        val suffix = when (keyword) {
            "return" -> {
                val fn = parameters.position.ancestorStrict<TolkFunction>() ?: return builder
                val returnTy = fn.returnTy
                if (returnTy == TolkTy.Void) ";" else " "
            }
            else -> " "
        }
        return builder.withInsertHandler { ctx, _ ->
            ctx.addSuffix(suffix)
        }
    }
}
