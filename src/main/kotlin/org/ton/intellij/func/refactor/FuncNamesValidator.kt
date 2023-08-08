package org.ton.intellij.func.refactor

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import org.ton.intellij.func.psi.FUNC_KEYWORDS

class FuncNamesValidator : NamesValidator {
    override fun isKeyword(name: String, project: Project?): Boolean {
        if (name.isBlank()) return false
        return FUNC_KEYWORDS.types.find {
            it.debugName == name
        } != null
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
