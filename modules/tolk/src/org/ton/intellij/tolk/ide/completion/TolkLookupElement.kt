package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator

class TolkLookupElement(
    delegate: LookupElement,
    val data: TolkLookupElementData
) : LookupElementDecorator<LookupElement>(delegate) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as TolkLookupElement

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }
}

fun LookupElement.toTolkLookupElement(data: TolkLookupElementData) = TolkLookupElement(this, data)

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

    val isSelfTypeNullableCompatible: Boolean = false,

    /**
     * `true` if the lookup element must show only if a completion result is empty.
     */
    val isDeferredLookup: Boolean = false,

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
        FROM_UNRESOLVED_IMPORT
    }
}
