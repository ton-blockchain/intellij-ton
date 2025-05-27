package org.ton.intellij.tolk.ide

import com.intellij.codeInsight.highlighting.PairedBraceMatcherAdapter
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.tree.TokenSet
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.parser.TolkParserDefinition
import org.ton.intellij.tolk.psi.TolkElementTypes

private val TYPE_TOKENS = TokenSet.orSet(
    TolkParserDefinition.WHITE_SPACE_OR_COMMENT_BIT_SET,
    TokenSet.create(
        TolkElementTypes.IDENTIFIER,
        TolkElementTypes.QUEST,
        TolkElementTypes.OR,
        TolkElementTypes.MAPSTO,
    )
)

class TolkPairedBraceMatcher : PairedBraceMatcherAdapter(
    TolkBraceMatcher(), TolkLanguage
) {
    override fun isLBraceToken(iterator: HighlighterIterator, fileText: CharSequence, fileType: FileType): Boolean {
        return isBrace(iterator, fileText, fileType, true)
    }

    override fun isRBraceToken(iterator: HighlighterIterator, fileText: CharSequence, fileType: FileType): Boolean {
        return isBrace(iterator, fileText, fileType, false)
    }

    private fun isBrace(
        iterator: HighlighterIterator,
        fileText: CharSequence,
        fileType: FileType,
        left: Boolean
    ): Boolean {
        val pair = findPair(left, iterator, fileText, fileType) ?: return false
        val opposite = if (left) TolkElementTypes.GT else TolkElementTypes.LT
        if ((if (left) pair.rightBraceType else pair.leftBraceType) != opposite) return true

        val braceElementType = if (left) TolkElementTypes.LT else TolkElementTypes.GT
        var count = 0

        try {
            var paired = 1
            while (true) {
                count++
                if (left) iterator.advance() else iterator.retreat()

                if (iterator.atEnd()) break

                when (val tokenType = iterator.tokenType) {
                    opposite -> {
                        if (--paired == 0) return true
                    }
                    braceElementType -> paired++
                    else -> {
                        val isAllowed = TYPE_TOKENS.contains(tokenType)
                        if (!isAllowed) return false
                    }
                }
            }
            return false
        } finally {
            repeat(count) {
                if (left) iterator.retreat() else iterator.advance()
            }
        }
    }
}
