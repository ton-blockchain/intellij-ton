package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.lookup.LookupElement

private typealias P = TolkLookupElementData

interface TolkCompletionWeigher {
    fun weigh(element: LookupElement): Comparable<*>

    val id: String

    companion object {
        val WEIGHERS = listOf(
            "priority",
            preferUpperVariant(P::keywordKind, id = "tolk-prefer-keywords"),
            preferTrue(P::isSelfTypeCompatible, id = "tolk-prefer-compatible-self-type"),
            preferTrue(P::isLocal, id = "tolk-prefer-locals"),
            preferUpperVariant(P::elementKind, id = "tolk-prefer-by-kind"),
            preferTrue(P::isSelfTypeNullableCompatible, id = "tolk-prefer-compatible-self-type-nullable"),
            preferTrue(P::isInherentUnionMember, id = "tolk-prefer-inherent-union-member"),
            preferTrue(P::isGeneric, id = "tolk-prefer-generic"),
            "prefix",
            "stats",
            "proximity",
            preferTrue(P::isDeferredLookup, id = "tolk-prefer-deferred-lookup"),
        )
    }
}

private fun preferTrue(
    property: (P) -> Boolean,
    id: String
): TolkCompletionWeigher = object : TolkCompletionWeigher {
    override fun weigh(element: LookupElement): Boolean =
        if (element is TolkLookupElement) !property(element.data) else true

    override val id: String get() = id
}

private fun preferUpperVariant(
    property: (P) -> Enum<*>,
    id: String
): TolkCompletionWeigher = object : TolkCompletionWeigher {
    override fun weigh(element: LookupElement): Int =
        if (element is TolkLookupElement) property(element.data).ordinal else Int.MAX_VALUE

    override val id: String get() = id
}
