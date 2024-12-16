package org.ton.intellij.tolk.doc

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.InlineLinkGeneratingProvider
import org.intellij.markdown.html.ReferenceLinksGeneratingProvider
import org.intellij.markdown.html.URI
import org.intellij.markdown.html.entities.EntityConverter
import org.intellij.markdown.parser.LinkMap

private fun linkIsProbablyValidTolkPath(link: CharSequence): Boolean {
    return link.none { it in "/.#" || it.isWhitespace() }
}

private fun markLinkAsLanguageItemIfItIsTolkPath(link: CharSequence): CharSequence {
    return if (linkIsProbablyValidTolkPath(link)) "${DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL}$link" else link
}

class TolkReferenceLinksGeneratingProvider(
    private val linkMap: LinkMap,
    baseURI: URI?,
    resolveAnchors: Boolean,
) : ReferenceLinksGeneratingProvider(linkMap, baseURI, resolveAnchors) {
    override fun renderLink(
        visitor: HtmlGenerator.HtmlGeneratingVisitor,
        text: String,
        node: ASTNode,
        info: RenderInfo,
    ) {
        super.renderLink(
            visitor,
            text,
            node,
            info.copy(destination = markLinkAsLanguageItemIfItIsTolkPath(info.destination))
        )
    }

    override fun getRenderInfo(text: String, node: ASTNode): RenderInfo? {
        val label = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_LABEL } ?: return null
        val labelText = label.getTextInNode(text)

        val linkInfo = linkMap.getLinkInfo(labelText)
        val (linkDestination, linkTitle) = if (linkInfo != null) {
            linkInfo.destination to linkInfo.title
        } else {
            // then maybe it's implied shortcut reference link, i.e. shortcut reference link without a matching link reference definition
            // so "[Iterator]" is the same as "[Iterator](Iterator)" and will be eventually rendered as "<a href="psi_element://Iterator">Iterator</a>"
            val linkText = labelText.removeSurrounding("[", "]").removeSurrounding("`")
            if (!linkIsProbablyValidTolkPath(linkText)) return null
            linkText to null
        }

        val linkTextNode = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
        return RenderInfo(
            linkTextNode ?: label,
            EntityConverter.replaceEntities(linkDestination, processEntities = true, processEscapes = true),
            linkTitle?.let { EntityConverter.replaceEntities(it, processEntities = true, processEscapes = true) }
        )
    }
}

open class TolkInlineLinkGeneratingProvider(baseURI: java.net.URI?, resolveAnchors: Boolean) :
    InlineLinkGeneratingProvider(baseURI, resolveAnchors) {
    override fun renderLink(
        visitor: HtmlGenerator.HtmlGeneratingVisitor,
        text: String,
        node: ASTNode,
        info: RenderInfo,
    ) {
        super.renderLink(
            visitor,
            text,
            node,
            info.copy(destination = markLinkAsLanguageItemIfItIsTolkPath(info.destination))
        )
    }
}
