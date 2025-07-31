package org.ton.intellij.tolk.ide

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.generation.CommenterDataHolder
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.lang.Commenter
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.BLOCK_COMMENT
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.DOC_BLOCK_COMMENT
import org.ton.intellij.tolk.parser.TolkParserDefinition.Companion.EOL_COMMENT

data class CommentHolder(val file: PsiFile) : CommenterDataHolder() {
    fun useSpaceAfterLineComment(): Boolean = CodeStyle.getLanguageSettings(file, TolkLanguage).LINE_COMMENT_ADD_SPACE
}

class TolkCommenter : Commenter, CodeDocumentationAwareCommenter {
    override fun isDocumentationComment(element: PsiComment?) = element?.tokenType == DOC_BLOCK_COMMENT
    override fun getDocumentationCommentTokenType(): IElementType = DOC_BLOCK_COMMENT
    override fun getDocumentationCommentLinePrefix(): String = "*"
    override fun getDocumentationCommentPrefix(): String = "/**"
    override fun getDocumentationCommentSuffix(): String = "*/"
    // act like there are no doc comments, these are handled in `TolkEnterInLineCommentHandler`
//    override fun isDocumentationComment(element: PsiComment?): Boolean = false
//    override fun getDocumentationCommentTokenType(): IElementType? = null
//    override fun getDocumentationCommentLinePrefix(): String? = null
//    override fun getDocumentationCommentPrefix(): String? = null
//    override fun getDocumentationCommentSuffix(): String? = null

    override fun getLineCommentTokenType(): IElementType = EOL_COMMENT
    override fun getBlockCommentTokenType(): IElementType = BLOCK_COMMENT

    override fun getLineCommentPrefix(): String = "//"

    override fun getBlockCommentPrefix(): String = "/*"
    override fun getBlockCommentSuffix(): String = "*/"

    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}
