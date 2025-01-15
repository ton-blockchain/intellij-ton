package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import org.ton.intellij.tolk.TolkLanguage

class TolkCharFilter : CharFilter() {
    override fun acceptChar(c: Char, prefixLength: Int, lookup: Lookup): Result? {
        val file = lookup.psiFile ?: return null
        if (!file.language.isKindOf(TolkLanguage)) return null

        val item = lookup.currentItem
        if (item == null || !item.isValid) return null

        if (c == ';' || c == ',' || c == '(' || c == ')' || c == ' ' || c == '~' || c == '.' || c == '"') {
            return Result.HIDE_LOOKUP
        }

        return Result.ADD_TO_PREFIX
    }
}
