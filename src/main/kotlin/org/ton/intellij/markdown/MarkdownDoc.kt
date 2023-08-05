package org.ton.intellij.markdown

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.util.CharTable
import com.intellij.util.text.CharArrayUtil
import com.intellij.util.text.CharSequenceSubSequence
import org.intellij.markdown.IElementType
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import kotlin.math.max
import kotlin.math.min

interface MarkdownPsiFactory {
    fun buildComposite(markdownElementType: IElementType): TreeElement?

    fun createText(charSequence: CharSequence): LeafPsiElement

    fun createGap(charSequence: CharSequence): LeafPsiElement

    fun createRoot(): CompositeElement
}

class MarkdownDocAstBuilder private constructor(
    private val textMap: MarkdownDocTextMap,
    private val charTable: CharTable,
    private val psiFactory: MarkdownPsiFactory,
) {
    private var prevNodeEnd = 0

    fun buildTree(root: CompositeElement, markdownRoot: org.intellij.markdown.ast.ASTNode) {
        for (markdownChild in markdownRoot.children) {
            visitNode(root, markdownChild)
        }

        if (prevNodeEnd < textMap.originalText.length) {
            root.insertLeaves(prevNodeEnd, textMap.originalText.length)
        }
    }

    private fun visitNode(parent: CompositeElement, markdownNode: org.intellij.markdown.ast.ASTNode) {
        val markdownType = markdownNode.type
        var node = psiFactory.buildComposite(markdownType)
        if (node == null) {
            // `null` means that we should skip the node. See docs for `mapMarkdownToRust`
            if (markdownNode !is org.intellij.markdown.ast.LeafASTNode) {
                for (markdownChild in markdownNode.children) {
                    visitNode(parent, markdownChild)
                }
            }
            return
        }

        parent.insertLeaves(markdownNode.startOffset)
        parent.rawAddChildrenWithoutNotifications(node)

        if (node is CompositeElement) {
            for (markdownChild in markdownNode.children) {
                visitNode(node, markdownChild)
            }
        }
        node.insertLeaves(markdownNode.endOffset)
    }


    private fun CompositeElement.insertLeaves(startOffset: Int, endOffset: Int) {
        textMap.processPiecesInRange(startOffset, endOffset) { piece ->
            val internedText = charTable.intern(piece.str)
            val element = when (piece.kind) {
                PieceKind.TEXT -> psiFactory.createText(internedText)
                PieceKind.GAP -> psiFactory.createGap(internedText)
                PieceKind.WHITESPACE -> PsiWhiteSpaceImpl(internedText)
            }
            rawAddChildrenWithoutNotifications(element)
        }
    }

    private fun TreeElement.insertLeaves(endOffset: Int) {
        val endOffsetMapped = textMap.mapOffsetFromMarkdown(endOffset)
        if (endOffsetMapped != prevNodeEnd && this is CompositeElement) {
            insertLeaves(prevNodeEnd, endOffsetMapped)
        }
        prevNodeEnd = endOffsetMapped
    }

    companion object {
        fun renderHtml(
            text: CharSequence,
            prefix: String,
            flavour: MarkdownFlavourDescriptor,
        ): String {
            val textMap = MarkdownDocTextMap(text, prefix)
            val markdownRoot =
                MarkdownParser(flavour).buildMarkdownTreeFromString(textMap.mappedText)
            return HtmlGenerator(textMap.mappedText, markdownRoot, flavour).generateHtml()
        }

        fun build(
            text: CharSequence,
            charTable: CharTable,
            prefix: String,
            psiFactory: MarkdownPsiFactory,
        ): ASTNode {
            val textMap = MarkdownDocTextMap(text, prefix)
            val root = psiFactory.createRoot()
            val markdownRoot =
                MarkdownParser(CommonMarkFlavourDescriptor()).buildMarkdownTreeFromString(textMap.mappedText)
            MarkdownDocAstBuilder(textMap, charTable, psiFactory).buildTree(root, markdownRoot)
            return root.firstChildNode
        }
    }
}

private class MarkdownDocTextMap private constructor(
    val originalText: CharSequence,
    val mappedText: String,
    private val offsetMap: IntArray, // mappedText -> originalText map
    private val pieces: List<Piece>,
) {
    fun mapOffsetFromMarkdown(offset: Int): Int = offsetMap[offset]

    private fun mapTextRangeFromMarkdownToRust(range: TextRange): TextRange = TextRange(
        mapOffsetFromMarkdown(range.startOffset),
        mapOffsetFromMarkdown(range.endOffset)
    )

    inline fun processPiecesInRange(startOffset: Int, endOffset: Int, processor: (Piece) -> Unit) {
        var offset = 0
        for (p in pieces) {
            val pieceEndOffset = offset + p.str.length
            if (startOffset < pieceEndOffset && endOffset - offset > 0) {
                processor(p.cut(startOffset - offset, endOffset - offset))
            }
            offset += p.str.length
        }
    }

    fun mapFully(range: TextRange): CharSequence? {
        processPiecesInRange(range.startOffset, range.endOffset) {
            return if (it.kind == PieceKind.TEXT && it.str.length == range.length) it.str else null
        }

        return null
    }

    companion object {
        operator fun invoke(text: CharSequence, prefix: String): MarkdownDocTextMap {
            val pieces = mutableListOf<Piece>()
            val mappedText = StringBuilder()
            val map = IntArray(text.length + 1)
            var textPosition = 0
            DocLine.removePrefix(text, prefix).forEach { line ->
                if (line.hasPrefix) {
                    val prefix = line.prefix
                    textPosition += prefix.length
                    pieces.mergeOrAddGapWithWS(prefix)
                }
                if (line.hasContent) {
                    val content = line.content
                    for (i in content.indices) {
                        map[mappedText.length + i] = textPosition + i
                    }
                    map[mappedText.length + content.length] = textPosition + content.length
                    textPosition += content.length
                    mappedText.append(content)
                    pieces += Piece(content, PieceKind.TEXT)
                }
                val hasLineBreak = !line.isLastLine && !line.hasSuffix
                if (hasLineBreak) {
                    if (!line.isRemoved) {
                        map[mappedText.length] = textPosition
                        map[mappedText.length + 1] = textPosition + 1
                        mappedText.append("\n")
                    }
                    textPosition += 1
                    pieces.mergeOrAddWS("\n")
                }
                if (line.hasSuffix) {
                    val suffix = line.suffix
                    textPosition += suffix.length
                    pieces.mergeOrAddGapWithWS(suffix)
                }
            }
            check(mappedText.length <= text.length)
            check(mappedText.length < map.size)
            check(mappedText.indices.all { map[it + 1] > map[it] })
            return MarkdownDocTextMap(text, mappedText.toString(), map, pieces)
        }

        private fun MutableList<Piece>.mergeOrAddGapWithWS(str: CharSequence) {
            val gapStart = CharArrayUtil.shiftForward(str, 0, "\n\t ")
            if (gapStart != 0) {
                mergeOrAddWS(str.subSequence(0, gapStart))
            }
            if (gapStart != str.length) {
                val gapEnd = CharArrayUtil.shiftBackward(str, gapStart, str.lastIndex, "\n\t ") + 1
                val gapText = str.subSequence(gapStart, gapEnd)
                check(gapText.isNotEmpty())
                this += Piece(gapText, PieceKind.GAP)
                if (gapEnd != str.length) {
                    mergeOrAddWS(str.subSequence(gapEnd, str.length))
                }
            }
        }

        private fun MutableList<Piece>.mergeOrAddWS(gap: CharSequence) {
            if (lastOrNull()?.kind == PieceKind.WHITESPACE) {
                this[lastIndex] = Piece(this[lastIndex].str.toString() + gap, PieceKind.WHITESPACE)
            } else {
                this += Piece(gap, PieceKind.WHITESPACE)
            }
        }
    }
}

private data class DocLine(
    val text: CharSequence,
    val startOffset: Int,
    val endOffset: Int,
    val contentStartOffset: Int = startOffset,
    val contentEndOffset: Int = endOffset,
    val isLastLine: Boolean,
    val isRemoved: Boolean = false,
) {
    val prefix: CharSequence get() = CharSequenceSubSequence(text, startOffset, contentStartOffset)
    val content: CharSequence get() = CharSequenceSubSequence(text, contentStartOffset, contentEndOffset)
    val suffix: CharSequence get() = CharSequenceSubSequence(text, contentEndOffset, endOffset)

    val hasPrefix: Boolean get() = prefixLength != 0
    val hasContent: Boolean get() = contentLength != 0
    val hasSuffix: Boolean get() = suffixLength != 0

    val prefixLength: Int get() = contentStartOffset - startOffset
    val contentLength: Int get() = contentEndOffset - contentStartOffset
    val suffixLength: Int get() = endOffset - contentEndOffset

    fun trimStart() = copy(contentStartOffset = shiftForwardWhitespace(), contentEndOffset = contentEndOffset)

    fun removePrefix(prefix: String) =
        if (CharArrayUtil.regionMatches(text, contentStartOffset, contentEndOffset, prefix)) {
            copy(contentStartOffset = contentStartOffset + prefix.length, contentEndOffset = contentEndOffset)
        } else {
            this
        }

    fun countStartWhitespace() = shiftForwardWhitespace() - contentStartOffset

    fun substring(startIndex: Int): DocLine {
        require(startIndex <= contentLength)
        return copy(contentStartOffset = contentStartOffset + startIndex)
    }

    private fun shiftForwardWhitespace() = CharArrayUtil.shiftForward(text, contentStartOffset, contentEndOffset, " \t")

    companion object {
        fun removePrefix(text: CharSequence, prefix: String) = splitLines(text).removePrefix(prefix)

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

private fun Sequence<DocLine>.removeCommonIndent(): Sequence<DocLine> {
    "".trimIndent()

    val lines = toList()
    val minIndent = lines.fold(Int.MAX_VALUE) { minIndent, line ->
        if (line.content.isBlank()) {
            minIndent
        } else {
            min(minIndent, line.countStartWhitespace())
        }
    }
    return lines.asSequence().map { line ->
        line.substring(min(minIndent, line.contentLength))
    }
}

private fun Sequence<DocLine>.removePrefix(prefix: String) =
    map { it.trimStart().removePrefix(prefix) }.removeCommonIndent()

private data class Piece(val str: CharSequence, val kind: PieceKind) {
    fun cut(startOffset: Int, endOffset: Int): Piece {
        val newStr = str.subSequence(max(0, startOffset), min(endOffset, str.length))
        return Piece(newStr, kind)
    }
}

private enum class PieceKind { TEXT, GAP, WHITESPACE }
