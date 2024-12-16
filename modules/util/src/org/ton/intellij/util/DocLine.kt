package org.ton.intellij.util

import com.intellij.util.text.CharArrayUtil
import com.intellij.util.text.CharSequenceSubSequence
import kotlin.math.min

data class DocLine(
    private val text: CharSequence,
    private val startOffset: Int,
    private val endOffset: Int,
    val contentStartOffset: Int = startOffset,
    private val contentEndOffset: Int = endOffset,
    val isLastLine: Boolean,
    val isRemoved: Boolean = false,
) {
    init {
        require(contentEndOffset >= contentStartOffset) { "`$text`, $contentStartOffset, $contentEndOffset" }
    }

    val prefix: CharSequence get() = CharSequenceSubSequence(text, startOffset, contentStartOffset)
    val content: CharSequence get() = CharSequenceSubSequence(text, contentStartOffset, contentEndOffset)
    val suffix: CharSequence get() = CharSequenceSubSequence(text, contentEndOffset, endOffset)

    val hasPrefix: Boolean get() = startOffset != contentStartOffset
    val hasContent: Boolean get() = contentStartOffset != contentEndOffset
    val hasSuffix: Boolean get() = contentEndOffset != endOffset

    val contentLength: Int get() = contentEndOffset - contentStartOffset

    fun removePrefix(prefix: String): DocLine {
        return if (CharArrayUtil.regionMatches(text, contentStartOffset, contentEndOffset, prefix)) {
            copy(contentStartOffset = contentStartOffset + prefix.length, contentEndOffset = contentEndOffset)
        } else {
            this
        }
    }

    fun removeSuffix(suffix: String): DocLine {
        if (contentLength < suffix.length) return this
        return if (CharArrayUtil.regionMatches(text, contentEndOffset - suffix.length, contentEndOffset, suffix)) {
            copy(contentStartOffset = contentStartOffset, contentEndOffset = contentEndOffset - suffix.length)
        } else {
            this
        }
    }

    fun markRemoved(): DocLine {
        return copy(contentEndOffset = contentStartOffset, isRemoved = true)
    }

    fun trimStart(): DocLine {
        val newOffset = shiftForwardWhitespace()
        return copy(contentStartOffset = newOffset, contentEndOffset = contentEndOffset)
    }

    fun countStartWhitespace(): Int {
        return shiftForwardWhitespace() - contentStartOffset
    }

    private fun shiftForwardWhitespace(): Int =
        CharArrayUtil.shiftForward(text, contentStartOffset, contentEndOffset, " \t")

    fun substring(startIndex: Int): DocLine {
        require(startIndex <= contentLength)
        return copy(contentStartOffset = contentStartOffset + startIndex)
    }

    companion object {
        fun splitLines(text: CharSequence): Sequence<DocLine> {
            var prev = 0
            return generateSequence {
                if (prev == -1) return@generateSequence null
                val index = text.indexOf(char = '\n', startIndex = prev)
                if (index >= 0) {
                    val line = DocLine(text, startOffset = prev, endOffset = index, isLastLine = false)
                    prev = index + 1
                    line
                } else {
                    val line = DocLine(text, startOffset = prev, endOffset = text.length, isLastLine = true)
                    prev = -1
                    line
                }
            }
        }
    }
}

public fun Sequence<DocLine>.removeEolDecoration(infix: String): Sequence<DocLine> =
    map { it.trimStart().removePrefix(infix) }.removeCommonIndent()

public fun Sequence<DocLine>.removeCommonIndent(): Sequence<DocLine> {
    val lines = toList()

    val minIndent = lines.fold(Int.MAX_VALUE) { minIndent, line ->
        if (line.isRemoved || line.content.isBlank()) {
            minIndent
        } else {
            min(minIndent, line.countStartWhitespace())
        }
    }

    return lines.asSequence().map { line ->
        line.substring(min(minIndent, line.contentLength))
    }
}

public fun Sequence<DocLine>.content() = mapNotNull { if (it.isRemoved) null else it.content }
