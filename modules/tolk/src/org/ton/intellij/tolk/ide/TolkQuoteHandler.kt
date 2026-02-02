package org.ton.intellij.tolk.ide

import com.intellij.codeInsight.editorActions.MultiCharQuoteHandler
import com.intellij.codeInsight.editorActions.QuoteHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import org.ton.intellij.tolk.psi.TolkElementTypes.*

class TolkQuoteHandler : QuoteHandler, MultiCharQuoteHandler {
    override fun isOpeningQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        if (iterator.tokenType == OPEN_QUOTE) {
            val start = iterator.start
            val end = iterator.end
            return end - start == 1
        }
        return false
    }

    override fun isClosingQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        return iterator.tokenType == CLOSING_QUOTE
    }

    override fun hasNonClosedLiteral(editor: Editor, iterator: HighlighterIterator, offset: Int): Boolean {
        return true
    }

    override fun isInsideLiteral(iterator: HighlighterIterator): Boolean {
        val tokenType = iterator.tokenType
        return tokenType == RAW_STRING_ELEMENT || tokenType == OPEN_QUOTE || tokenType == CLOSING_QUOTE || tokenType == ESCAPE_SEQUENCE
    }

    override fun getClosingQuote(iterator: HighlighterIterator, offset: Int): CharSequence? {
        val start = iterator.start
        val end = iterator.end
        val text = iterator.document.charsSequence
        if (iterator.tokenType == OPEN_QUOTE && end - start == 3) {
            return "\"\"\""
        }
        if (offset >= 3 && text.subSequence(offset - 3, offset) == "\"\"\"") {
            return "\"\"\""
        }
        return null
    }
}
