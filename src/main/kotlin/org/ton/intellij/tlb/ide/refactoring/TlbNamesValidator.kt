package org.ton.intellij.tlb.ide.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project

private val INT_REGEX = Regex.fromLiteral("int[0-9]+")
private val UINT_REGEX = Regex.fromLiteral("uint[0-9]+")
private val BITS_REGEX = Regex.fromLiteral("bits[0-9]+")

class TlbNamesValidator : NamesValidator {
    override fun isKeyword(name: String, project: Project): Boolean {
        when (name) {
            "Any", "Cell", "Type" -> return true
            else -> {
                if (name.startsWith("int")) {
                    INT_REGEX.matchEntire(name)?.let { return true }
                } else if (name.startsWith("uint")) {
                    UINT_REGEX.matchEntire(name)?.let { return true }
                } else if (name.startsWith("bits")) {
                    BITS_REGEX.matchEntire(name)?.let { return true }
                }
            }
        }
        return false
    }

    override fun isIdentifier(name: String, project: Project?): Boolean {
        return name.isNotEmpty() && name[0].isLetter()
                || name[0] == '_'
                || (name.length > 1 && name[0] == '!' && name[1].isLetter())
    }
}
