package org.ton.intellij.tolk.doc

import com.intellij.openapi.util.TextRange
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.InlineDocumentation
import com.intellij.platform.backend.documentation.InlineDocumentationProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.Nls
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.psi.TolkDocOwner
import org.ton.intellij.tolk.psi.TolkFile

class TolkInlineDocumentationProvider : InlineDocumentationProvider {
    override fun inlineDocumentationItems(file: PsiFile?): Collection<InlineDocumentation> {
        if (file !is TolkFile) return emptyList()

        val result = mutableListOf<InlineDocumentation>()
        PsiTreeUtil.processElements(file) {
            val docOwner = it as? TolkDocOwner
            val doc = docOwner?.doc
            if (doc != null) {
                result.add(TolkInlineDocumentation(doc, docOwner))
            }
            true
        }
        return result
    }

    override fun findInlineDocumentation(
        file: PsiFile,
        textRange: TextRange
    ): InlineDocumentation? {
        val comment = PsiTreeUtil.getParentOfType(file.findElementAt(textRange.startOffset), TolkDocComment::class.java) ?: return null
        if (comment.textRange == textRange) {
            val declaration = comment.owner ?: return null
            return TolkInlineDocumentation(comment, declaration)
        }
        return null
    }
}


@Suppress("UnstableApiUsage")
class TolkInlineDocumentation(
    val comment: TolkDocComment,
    val owner: TolkDocOwner
) : InlineDocumentation {
    override fun getDocumentationRange(): TextRange = comment.textRange

    override fun getDocumentationOwnerRange(): TextRange? = owner.textRange

    override fun renderText(): @Nls String? {



        return null
    }

    override fun getOwnerTarget(): DocumentationTarget = TolkDocumentationTarget(owner, owner)
}
