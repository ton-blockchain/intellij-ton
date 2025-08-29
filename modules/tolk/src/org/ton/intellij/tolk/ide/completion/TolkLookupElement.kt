package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator

class TolkLookupElement(
    delegate: LookupElement,
    val data: TolkLookupElementData
) : LookupElementDecorator<LookupElement>(delegate) {
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (javaClass != o?.javaClass) return false
        if (!super.equals(o)) return false

        o as TolkLookupElement

        return data == o.data
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }
}

fun LookupElement.toTolkLookupElement(data: TolkLookupElementData) = TolkLookupElement(this, data)
fun LookupElement.withPriority(priority: Double) = PrioritizedLookupElement.withPriority(this, priority)

data class TolkLookupElementData(
    /**
     * [KeywordKind.NOT_A_KEYWORD] if the lookup element is *not* a keyword.
     * Some another [KeywordKind] variant if the lookup element *is* a keyword.
     */
    val keywordKind: KeywordKind = KeywordKind.NOT_A_KEYWORD,

    /**
     * `false` if the lookup element is a method that has a `self` type incompatible with a receiver type,
     *
     */
    val isSelfTypeCompatible: Boolean = true,

    val isSelfTypeNullableCompatible: Boolean = true,

    /**
     * `true` if the lookup element refers to a local declaration, e.g. inside a function body
     */
    val isLocal: Boolean = false,

    /**
     * `true` if the lookup element is a member of union
     */
    val isInherentUnionMember: Boolean = false,

    /**
     * `true` if the lookup element is a inferred of a generic type
     */
    val isGeneric: Boolean = false,

    /**
     * Some classification of an element
     */
    val elementKind: ElementKind = ElementKind.DEFAULT
) {
    enum class KeywordKind {
        // Top Priority
        ELSE_BRANCH,
        CONTEXT_RETURN_KEYWORD,
        KEYWORD,
        NOT_A_KEYWORD,
        // Least priority
    }

    enum class ElementKind {
        VARIABLE,
        FIELD,
        STATIC_FUNCTION,
        DEFAULT,
        DEPRECATED,
        ENTRY_POINT_FUNCTION,
        FROM_UNRESOLVED_IMPORT,
        LOW_LEVEL_METHOD,
    }
}
