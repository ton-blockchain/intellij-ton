package org.ton.intellij.tolk.doc

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.impl.source.tree.SharedImplUtil
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.tree.ILazyParseableElementType
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_CODE_BLOCK
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_CODE_FENCE
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_CODE_FENCE_LANG
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_CODE_FENCE_START_END
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_CODE_SPAN
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_FULL_REFERENCE_LINK
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_GAP
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_INLINE_LINK
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_LINK_DEFINITION
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_LINK_DESTINATION
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_LINK_LABEL
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_LINK_TEXT
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_LINK_TITLE
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_SHORT_REFERENCE_LINK
import org.ton.intellij.tolk.doc.psi.TolkDocElementTypes.DOC_TEXT
import org.ton.intellij.tolk.doc.psi.impl.TolkDocCommentImpl
import org.ton.intellij.tolk.doc.psi.impl.TolkDocGapImpl
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.markdown.MarkdownDocAstBuilder
import org.ton.intellij.markdown.MarkdownPsiFactory


;
class TolkDocCommentElementType(debugName: String) : ILazyParseableElementType(debugName, TolkLanguage) {
    val factory = object : MarkdownPsiFactory {
        override fun buildComposite(markdownElementType: IElementType): TreeElement? = when (markdownElementType) {
            MarkdownElementTypes.CODE_SPAN -> DOC_CODE_SPAN.createCompositeNode()
            MarkdownElementTypes.CODE_FENCE -> DOC_CODE_FENCE.createCompositeNode()
            MarkdownElementTypes.CODE_BLOCK -> DOC_CODE_BLOCK.createCompositeNode()
            MarkdownTokenTypes.FENCE_LANG -> DOC_CODE_FENCE_LANG.createCompositeNode()
            MarkdownTokenTypes.CODE_FENCE_START,
            MarkdownTokenTypes.CODE_FENCE_END -> DOC_CODE_FENCE_START_END.createCompositeNode()

            MarkdownElementTypes.INLINE_LINK -> DOC_INLINE_LINK.createCompositeNode()
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> DOC_SHORT_REFERENCE_LINK.createCompositeNode()
            MarkdownElementTypes.FULL_REFERENCE_LINK -> DOC_FULL_REFERENCE_LINK.createCompositeNode()
            MarkdownElementTypes.LINK_DEFINITION -> DOC_LINK_DEFINITION.createCompositeNode()

            MarkdownElementTypes.LINK_TEXT -> DOC_LINK_TEXT.createCompositeNode()
            MarkdownElementTypes.LINK_LABEL -> DOC_LINK_LABEL.createCompositeNode()
            MarkdownElementTypes.LINK_TITLE -> DOC_LINK_TITLE.createCompositeNode()
            MarkdownElementTypes.LINK_DESTINATION -> DOC_LINK_DESTINATION.createCompositeNode()
            MarkdownTokenTypes.LBRACKET -> LeafPsiElement(TolkElementTypes.LBRACK, "[")
            MarkdownTokenTypes.RBRACKET -> LeafPsiElement(TolkElementTypes.RBRACK, "]")
            MarkdownTokenTypes.LPAREN -> LeafPsiElement(TolkElementTypes.LPAREN, "(")
            MarkdownTokenTypes.RPAREN -> LeafPsiElement(TolkElementTypes.LPAREN, ")")
            else -> {
                null
            }
        }

        override fun createText(charSequence: CharSequence): LeafPsiElement = LeafPsiElement(DOC_TEXT, charSequence)

        override fun createGap(charSequence: CharSequence) = TolkDocGapImpl(DOC_GAP, charSequence)

        override fun createRoot(): CompositeElement = createNode(null)
    }

    override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode {
        val charTable = SharedImplUtil.findCharTableByTree(chameleon)
        return MarkdownDocAstBuilder.build(
            chameleon.chars,
            charTable,
            "///",
            factory
        )
    }

    override fun createNode(text: CharSequence?) =
        TolkDocCommentImpl(this, text)
}
