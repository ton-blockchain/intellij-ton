package org.ton.intellij.func.refactor

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project

class FuncNamesValidator : NamesValidator {
    override fun isKeyword(name: String, project: Project?): Boolean {
        return when (name) {
            "return",
            "var",
            "repeat",
            "do",
            "while",
            "until",
            "try",
            "catch",
            "if",
            "ifnot",
            "then",
            "else",
            "elseif",
            "elseifnot",
            "type",
            "forall",
            "extern",
            "global",
            "const",
            "asm",
            "impure",
            "inline",
            "inline_ref",
            "method_id",
            "infix",
            "infixl",
            "infixr",
            "operator",
            "auto_apply" -> true

            else -> false
        }
    }

    override fun isIdentifier(name: String, project: Project?): Boolean {
        if (name.isBlank()) return false
        if (name.first() == '\"') return false
        if (name.first() == '`' && name.last() == '`') {
            return name.length >= 3
        }
        if (name.startsWith("{-")) return false
        if (name.startsWith("\"\"\"") && name.endsWith("\"\"\"")) return false
        for ((index, currentChar) in name.withIndex()) {
            if (currentChar.isWhitespace()) return false
            if (index == 0 && currentChar == '~') continue
            if (currentChar in BAN_CHARS) return false
        }
        return true
    }

    companion object {
        private const val BAN_CHARS = ";.().~"
    }
}
