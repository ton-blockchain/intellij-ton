package org.ton.intellij.tolk.debug

internal object TolkDapPresentationFormatter {
    fun formatFrameName(rawName: String): String {
        return rawName.ifBlank { "<frame>" }
    }

    fun formatFrameLocation(sourceName: String?, line: Int, column: Int): String? {
        val normalizedSource = sourceName?.takeIf { it.isNotBlank() } ?: return null
        if (line <= 0) return normalizedSource
        return if (column > 0) "$normalizedSource:$line:$column" else "$normalizedSource:$line"
    }

    fun formatVariableType(rawType: String?): String? {
        return rawType?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun isNumericValue(valueText: String): Boolean {
        return valueText.matches(NUMERIC_VALUE_REGEX)
    }

    fun isStringValue(valueText: String): Boolean {
        return valueText.length >= 2 && valueText.first() == '"' && valueText.last() == '"'
    }

    fun isKeywordValue(valueText: String): Boolean {
        return valueText in KEYWORD_VALUES
    }

    fun shouldAutoExpandScope(name: String, index: Int, expensive: Boolean): Boolean {
        if (expensive) return false
        val normalized = name.trim().lowercase()
        if (normalized in AUTO_EXPAND_SCOPE_NAMES) return true
        return index == 0
    }

    fun formatVariableValue(
        rawValue: String?,
        hasChildren: Boolean,
        namedChildren: Int? = null,
        indexedChildren: Int? = null
    ): String {
        return formatVariableDisplay(rawValue, hasChildren, namedChildren, indexedChildren).valueText
    }

    fun formatVariableDisplay(
        rawValue: String?,
        hasChildren: Boolean,
        namedChildren: Int? = null,
        indexedChildren: Int? = null
    ): VariableDisplay {
        val normalized = rawValue.orEmpty()
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\n', ' ')
            .replace('\t', ' ')
            .trim()
            .replace(WHITESPACE_REGEX, " ")

        if (normalized.isEmpty()) {
            return VariableDisplay(
                valueText = if (hasChildren) formatStructuredPlaceholder(namedChildren, indexedChildren) else ""
            )
        }

        if (normalized == "<optimized out>" || normalized == "<not loaded>") {
            return VariableDisplay(valueText = normalized, commentOnly = true)
        }

        if (normalized.endsWith(LAST_SEEN_SUFFIX) && normalized.length > LAST_SEEN_SUFFIX.length) {
            return VariableDisplay(
                valueText = normalized.removeSuffix(LAST_SEEN_SUFFIX),
                commentSuffix = LAST_SEEN_SUFFIX
            )
        }

        return VariableDisplay(valueText = normalized)
    }

    private fun formatStructuredPlaceholder(namedChildren: Int?, indexedChildren: Int?): String {
        val parts = buildList {
            if ((namedChildren ?: 0) > 0) add("${namedChildren} named")
            if ((indexedChildren ?: 0) > 0) add("${indexedChildren} indexed")
        }
        return if (parts.isEmpty()) "{...}" else "{${parts.joinToString(", ")}}"
    }

    data class VariableDisplay(
        val valueText: String,
        val commentSuffix: String? = null,
        val commentOnly: Boolean = false
    )

    private val AUTO_EXPAND_SCOPE_NAMES = setOf(
        "locals",
        "local variables",
        "arguments",
        "args",
        "storage",
        "state",
        "globals",
        "context"
    )

    private val WHITESPACE_REGEX = Regex("\\s+")
    private val NUMERIC_VALUE_REGEX = Regex(
        pattern = "[+-]?(?:\\d+(?:\\.\\d+)?|0[xX][0-9a-fA-F_]+|0[bB][01_]+)"
    )
    private val KEYWORD_VALUES = setOf("true", "false", "null")
    private const val LAST_SEEN_SUFFIX = " (last seen)"
}
