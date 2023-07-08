package org.ton.intellij.func

import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.intellij.psi.tree.IElementType
import org.ton.intellij.func.psi.FuncElementTypes

class FuncCommenter : CodeDocumentationAwareCommenter {
    override fun getLineCommentPrefix(): String = ";;"

    override fun getBlockCommentPrefix(): String = "{-"

    override fun getBlockCommentSuffix(): String = "-}"

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null

    override fun getLineCommentTokenType(): IElementType = FuncElementTypes.LINE_COMMENT

    override fun getBlockCommentTokenType(): IElementType = FuncElementTypes.BLOCK_COMMENT

    override fun getDocumentationCommentTokenType(): IElementType = FuncElementTypes.DOC_COMMENT

    override fun getDocumentationCommentPrefix(): String = "{--"

    override fun getDocumentationCommentLinePrefix(): String? = null

    override fun getDocumentationCommentSuffix(): String = "--}"

    override fun isDocumentationComment(element: PsiComment?): Boolean =
        element?.tokenType == FuncElementTypes.DOC_COMMENT
}
