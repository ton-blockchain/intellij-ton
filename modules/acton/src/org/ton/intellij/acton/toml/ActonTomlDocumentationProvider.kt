package org.ton.intellij.acton.toml

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.html.SimpleTagProvider
import org.intellij.markdown.html.URI
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlTable
import kotlin.collections.set

class ActonTomlDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val keySegment = element as? TomlKeySegment ?: return null
        if (keySegment.containingFile.name != "Acton.toml") return null

        val table = keySegment.parent?.parent?.parent as? TomlTable ?: return null
        val segments = table.header.key?.segments ?: return null
        if (segments.isEmpty() || segments[0].name != "lint") return null

        val ruleName = keySegment.name ?: return null
        val project = element.project
        val rules = ActonLintRulesProvider.getLintRules(project)
        val rule = rules.find { it.name == ruleName } ?: return null

        val flavour = ActonDocMarkdownFlavourDescriptor()
        val markdownRoot = MarkdownParser(flavour).buildMarkdownTreeFromString(rule.description)
        return HtmlGenerator(rule.description, markdownRoot, flavour).generateHtml()
    }

    override fun getDocumentationElementForLookupItem(
        psiManager: com.intellij.psi.PsiManager?,
        `object`: Any?,
        element: PsiElement?
    ): PsiElement? {
        return element
    }
}

class ActonDocMarkdownFlavourDescriptor(
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
        return generatingProviders
    }
}