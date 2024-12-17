package org.ton.intellij.func.doc

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.SimpleTagProvider
import org.intellij.markdown.html.URI
import org.intellij.markdown.parser.LinkMap

class FuncDocMarkdownFlavourDescriptor(
    private val gfm: MarkdownFlavourDescriptor = GFMFlavourDescriptor(
        useSafeLinks = false,
        absolutizeAnchorLinks = true
    ),
) : MarkdownFlavourDescriptor by gfm {
    override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?): Map<IElementType, GeneratingProvider> {
        val generatingProviders = HashMap(gfm.createHtmlGeneratingProviders(linkMap, baseURI))
        // Filter out MARKDOWN_FILE to avoid producing unnecessary <body> tags
        generatingProviders.remove(MarkdownElementTypes.MARKDOWN_FILE)
        // h1 and h2 are too large
        generatingProviders[MarkdownElementTypes.ATX_1] = SimpleTagProvider("h2")
        generatingProviders[MarkdownElementTypes.ATX_2] = SimpleTagProvider("h3")

        generatingProviders[MarkdownElementTypes.SHORT_REFERENCE_LINK] =
            FuncReferenceLinksGeneratingProvider(linkMap, baseURI, resolveAnchors = true)
        generatingProviders[MarkdownElementTypes.FULL_REFERENCE_LINK] =
            FuncReferenceLinksGeneratingProvider(linkMap, baseURI, resolveAnchors = true)
        generatingProviders[MarkdownElementTypes.INLINE_LINK] =
            FuncInlineLinkGeneratingProvider(baseURI, resolveAnchors = true)

        return generatingProviders
    }
}
