package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.NlsContexts
import com.intellij.patterns.ElementPattern

class DeferredCompletionResultSet(
    val delegate: CompletionResultSet,
) : CompletionResultSet(
    delegate.prefixMatcher,
    delegate.consumer,
    delegate.contributor
) {
    private val deferredElements = mutableListOf<LookupElement>()
    private var passedElements = 0

    override fun addElement(element: LookupElement) {
        if (element is TolkLookupElement && element.data.isDeferredLookup) {
            deferredElements.add(element)
        } else {
            delegate.addElement(element)
            passedElements++
        }
    }

    fun flushDeferredElements() {
        if (deferredElements.isNotEmpty()) {
            if (passedElements == 0) {
                deferredElements.forEach { delegate.addElement(it) }
                deferredElements.clear()
            } else {
                delegate.restartCompletionWhenNothingMatches()
            }
        }
    }

    override fun withPrefixMatcher(matcher: PrefixMatcher): DeferredCompletionResultSet =
        DeferredCompletionResultSet(delegate.withPrefixMatcher(matcher))

    override fun withPrefixMatcher(prefix: String): DeferredCompletionResultSet =
        DeferredCompletionResultSet(delegate.withPrefixMatcher(prefix))

    override fun withRelevanceSorter(sorter: CompletionSorter): DeferredCompletionResultSet =
        DeferredCompletionResultSet(delegate.withRelevanceSorter(sorter))

    override fun addLookupAdvertisement(text: @NlsContexts.PopupAdvertisement String) {
        delegate.addLookupAdvertisement(text)
    }

    override fun caseInsensitive(): DeferredCompletionResultSet =
        DeferredCompletionResultSet(delegate.caseInsensitive())

    override fun restartCompletionOnPrefixChange(prefixCondition: ElementPattern<String?>?) {
        delegate.restartCompletionOnPrefixChange(prefixCondition)
    }

    override fun restartCompletionWhenNothingMatches() {
        delegate.restartCompletionWhenNothingMatches()
    }
}
