package org.ton.intellij.tolk.ide

import com.intellij.codeInsight.editorActions.QuoteHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import org.ton.intellij.tolk.psi.TolkElementTypes.*

class TolkQuoteHandler : QuoteHandler {
    override fun isClosingQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        return iterator.tokenType == CLOSING_QUOTE
    }

    override fun isOpeningQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        return iterator.tokenType == OPEN_QUOTE
    }

    override fun hasNonClosedLiteral(editor: Editor?, iterator: HighlighterIterator?, offset: Int): Boolean {
        return true
    }

    override fun isInsideLiteral(iterator: HighlighterIterator): Boolean {
        val tokenType = iterator.tokenType
        return tokenType == RAW_STRING_ELEMENT || tokenType == OPEN_QUOTE || tokenType == CLOSING_QUOTE
    }
}
