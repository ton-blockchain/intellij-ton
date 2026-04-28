package org.ton.intellij.acton.toml

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable

internal data class ActonTomlValueContext(
    val literal: TomlLiteral,
    val keyValue: TomlKeyValue,
    val path: List<String>,
    val isArrayItem: Boolean,
) {
    fun matches(vararg parts: String?): Boolean {
        if (path.size != parts.size) return false
        return path.indices.all { index ->
            parts[index] == null || path[index] == parts[index]
        }
    }
}

internal fun findActonTomlValueContext(element: PsiElement): ActonTomlValueContext? {
    val literal = PsiTreeUtil.getParentOfType(element, TomlLiteral::class.java, false) ?: return null
    if (literal.containingFile.name != "Acton.toml") return null

    val keyValue = PsiTreeUtil.getParentOfType(literal, TomlKeyValue::class.java, false) ?: return null
    val path = buildActonTomlPath(literal)
    if (path.isEmpty()) return null

    return ActonTomlValueContext(
        literal = literal,
        keyValue = keyValue,
        path = path,
        isArrayItem = keyValue.value != literal,
    )
}

private fun buildActonTomlPath(element: PsiElement): List<String> {
    val path = mutableListOf<String>()
    var current: PsiElement? = element
    while (current != null) {
        when (current) {
            is TomlKeyValue -> {
                val segments = current.key.segments.map { it.name ?: it.text }
                path.addAll(0, segments)
            }

            is TomlTable -> {
                val segments = current.header.key?.segments?.map { it.name ?: it.text }.orEmpty()
                path.addAll(0, segments)
            }
        }
        current = current.parent
    }
    return path
}

internal fun TomlLiteral.valueTextRange(): TextRange {
    val text = text
    return when {
        text.length >= 6 && text.startsWith("\"\"\"") && text.endsWith("\"\"\"") -> TextRange(3, text.length - 3)
        text.length >= 6 && text.startsWith("'''") && text.endsWith("'''") -> TextRange(3, text.length - 3)
        text.length >= 2 && text.startsWith("\"") && text.endsWith("\"") -> TextRange(1, text.length - 1)
        text.length >= 2 && text.startsWith("'") && text.endsWith("'") -> TextRange(1, text.length - 1)
        else -> TextRange(0, text.length)
    }
}

internal fun TomlLiteral.stringValue(): String {
    val text = text
    return when {
        text.length >= 6 && text.startsWith("\"\"\"") && text.endsWith("\"\"\"") -> text.substring(3, text.length - 3)
        text.length >= 6 && text.startsWith("'''") && text.endsWith("'''") -> text.substring(3, text.length - 3)
        text.length >= 2 && text.startsWith("\"") && text.endsWith("\"") -> text.substring(1, text.length - 1)
        text.length >= 2 && text.startsWith("'") && text.endsWith("'") -> text.substring(1, text.length - 1)
        else -> text
    }
}
