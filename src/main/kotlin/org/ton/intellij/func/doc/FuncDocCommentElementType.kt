package org.ton.intellij.func.doc

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
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.doc.psi.FuncDocElementTypes.DOC_CODE_SPAN
import org.ton.intellij.func.doc.psi.FuncDocElementTypes.DOC_FULL_REFERENCE_LINK
import org.ton.intellij.func.doc.psi.FuncDocElementTypes.DOC_GAP
import org.ton.intellij.func.doc.psi.FuncDocElementTypes.DOC_INLINE_LINK
import org.ton.intellij.func.doc.psi.FuncDocElementTypes.DOC_LINK_DEFINITION
import org.ton.intellij.func.doc.psi.FuncDocElementTypes.DOC_LINK_DESTINATION
import org.ton.intellij.func.doc.psi.FuncDocElementTypes.DOC_LINK_LABEL
import org.ton.intellij.func.doc.psi.FuncDocElementTypes.DOC_LINK_TEXT
import org.ton.intellij.func.doc.psi.FuncDocElementTypes.DOC_LINK_TITLE
import org.ton.intellij.func.doc.psi.FuncDocElementTypes.DOC_SHORT_REFERENCE_LINK
import org.ton.intellij.func.doc.psi.FuncDocElementTypes.DOC_TEXT
import org.ton.intellij.func.doc.psi.impl.FuncDocCommentImpl
import org.ton.intellij.func.doc.psi.impl.FuncDocGapImpl
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.markdown.MarkdownDocAstBuilder
import org.ton.intellij.markdown.MarkdownPsiFactory

class FuncDocCommentElementType(debugName: String) : ILazyParseableElementType(debugName, FuncLanguage) {
    val factory = object : MarkdownPsiFactory {
        override fun buildComposite(markdownElementType: IElementType): TreeElement? = when (markdownElementType) {
            MarkdownElementTypes.CODE_SPAN -> DOC_CODE_SPAN.createCompositeNode()

            MarkdownElementTypes.INLINE_LINK -> DOC_INLINE_LINK.createCompositeNode()
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> DOC_SHORT_REFERENCE_LINK.createCompositeNode()
            MarkdownElementTypes.FULL_REFERENCE_LINK -> DOC_FULL_REFERENCE_LINK.createCompositeNode()
            MarkdownElementTypes.LINK_DEFINITION -> DOC_LINK_DEFINITION.createCompositeNode()

            MarkdownElementTypes.LINK_TEXT -> DOC_LINK_TEXT.createCompositeNode()
            MarkdownElementTypes.LINK_LABEL -> DOC_LINK_LABEL.createCompositeNode()
            MarkdownElementTypes.LINK_TITLE -> DOC_LINK_TITLE.createCompositeNode()
            MarkdownElementTypes.LINK_DESTINATION -> DOC_LINK_DESTINATION.createCompositeNode()
            MarkdownTokenTypes.LBRACKET -> LeafPsiElement(FuncElementTypes.LBRACK, "[")
            MarkdownTokenTypes.RBRACKET -> LeafPsiElement(FuncElementTypes.RBRACK, "]")
            MarkdownTokenTypes.LPAREN -> LeafPsiElement(FuncElementTypes.LPAREN, "(")
            MarkdownTokenTypes.RPAREN -> LeafPsiElement(FuncElementTypes.LPAREN, ")")
            else -> {
//                println("build markdown: $markdownElementType")
                null
            }
        }

        override fun createText(charSequence: CharSequence): LeafPsiElement = LeafPsiElement(DOC_TEXT, charSequence)

        override fun createGap(charSequence: CharSequence) = FuncDocGapImpl(DOC_GAP, charSequence)

        override fun createRoot(): CompositeElement = createNode(null)
    }

    override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode {
        val charTable = SharedImplUtil.findCharTableByTree(chameleon)
        return MarkdownDocAstBuilder.build(
            chameleon.chars,
            charTable,
            ";;;",
            factory
        )
    }

    override fun createNode(text: CharSequence?) =
        FuncDocCommentImpl(this, text)
}
