package org.ton.intellij.func.ide

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.generation.CommenterDataHolder
import com.intellij.codeInsight.generation.SelfManagingCommenter
import com.intellij.codeInsight.generation.SelfManagingCommenterUtil
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.util.text.CharArrayUtil
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.parser.FuncParserDefinition.Companion.BLOCK_COMMENT

data class CommentHolder(val file: PsiFile) : CommenterDataHolder() {
    fun useSpaceAfterLineComment(): Boolean = CodeStyle.getLanguageSettings(file, FuncLanguage).LINE_COMMENT_ADD_SPACE
}

class FuncCommenter : CodeDocumentationAwareCommenter, SelfManagingCommenter<CommentHolder> {
    override fun isDocumentationComment(element: PsiComment?) = false
    override fun getDocumentationCommentTokenType(): IElementType? = null
    override fun getDocumentationCommentLinePrefix(): String? = null
    override fun getDocumentationCommentPrefix(): String? = null
    override fun getDocumentationCommentSuffix(): String? = null

    override fun getLineCommentTokenType(): IElementType? = null
    override fun getBlockCommentTokenType(): IElementType = BLOCK_COMMENT

    override fun getLineCommentPrefix(): String = ";;"

    override fun getBlockCommentPrefix(): String = "{-"
    override fun getBlockCommentSuffix(): String = "-}"

    // unused because we implement SelfManagingCommenter
    override fun getCommentedBlockCommentPrefix(): String = "*//*"
    override fun getCommentedBlockCommentSuffix(): String = "*//*"

    override fun getBlockCommentPrefix(
        selectionStart: Int,
        document: Document,
        data: CommentHolder,
    ): String = blockCommentPrefix

    override fun getBlockCommentSuffix(
        selectionEnd: Int,
        document: Document,
        data: CommentHolder,
    ): String = blockCommentSuffix

    override fun getBlockCommentRange(
        selectionStart: Int,
        selectionEnd: Int,
        document: Document,
        data: CommentHolder,
    ): TextRange? = SelfManagingCommenterUtil.getBlockCommentRange(
        selectionStart,
        selectionEnd,
        document,
        blockCommentPrefix,
        blockCommentSuffix
    )

    override fun insertBlockComment(
        startOffset: Int,
        endOffset: Int,
        document: Document,
        data: CommentHolder?,
    ): TextRange {
        return SelfManagingCommenterUtil.insertBlockComment(
            startOffset,
            endOffset,
            document,
            blockCommentPrefix,
            blockCommentSuffix
        )
    }

    override fun uncommentBlockComment(
        startOffset: Int,
        endOffset: Int,
        document: Document,
        data: CommentHolder?,
    ) = SelfManagingCommenterUtil.uncommentBlockComment(
        startOffset,
        endOffset,
        document,
        blockCommentPrefix,
        blockCommentSuffix
    )

    override fun isLineCommented(line: Int, offset: Int, document: Document, data: CommentHolder): Boolean {
        return LINE_PREFIXES.any { CharArrayUtil.regionMatches(document.charsSequence, offset, it) }
    }

    override fun commentLine(line: Int, offset: Int, document: Document, data: CommentHolder) {
        val addSpace = data.useSpaceAfterLineComment()
        document.insertString(offset, lineCommentPrefix + if (addSpace) " " else "")
    }

    override fun uncommentLine(line: Int, offset: Int, document: Document, data: CommentHolder) {
        val prefixLen = LINE_PREFIXES.find { CharArrayUtil.regionMatches(document.charsSequence, offset, it) }?.length
            ?: return
        val hasSpace = data.useSpaceAfterLineComment() &&
                CharArrayUtil.regionMatches(document.charsSequence, offset + prefixLen, " ")
        document.deleteString(offset, offset + prefixLen + if (hasSpace) 1 else 0)
    }

    override fun getCommentPrefix(line: Int, document: Document, data: CommentHolder): String = lineCommentPrefix

    override fun createBlockCommentingState(
        selectionStart: Int,
        selectionEnd: Int,
        document: Document,
        file: PsiFile,
    ): CommentHolder = CommentHolder(file)

    override fun createLineCommentingState(
        startLine: Int,
        endLine: Int,
        document: Document,
        file: PsiFile,
    ): CommentHolder = CommentHolder(file)

    companion object {
        private val LINE_PREFIXES: List<String> = listOf(";;;", ";;")
    }
}
