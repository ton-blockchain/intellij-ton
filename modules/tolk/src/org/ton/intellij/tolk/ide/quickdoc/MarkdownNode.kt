package org.ton.intellij.tolk.ide.quickdoc

import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.Language
import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.util.applyIf
import com.intellij.xml.util.XmlStringUtil
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.doc.TolkDocumentationProvider
import org.ton.intellij.tolk.psi.TolkFunction

class MarkdownNode(
    val node: org.intellij.markdown.ast.ASTNode,
    val parent: MarkdownNode?,
    val content: String,
    val owner: PsiElement,
) {
    val children: List<MarkdownNode> = node.children.map { MarkdownNode(it, this, content, owner) }
    val endOffset: Int get() = node.endOffset
    val startOffset: Int get() = node.startOffset
    val type: IElementType get() = node.type
    val text: String get() = content.substring(startOffset, endOffset)

    fun child(type: IElementType): MarkdownNode? = children.firstOrNull { it.type == type }

    fun visit(action: (MarkdownNode, () -> Unit) -> Unit) {
        action(this) {
            for (child in children) {
                child.visit(action)
            }
        }
    }

    fun toHtml(): String {
        if (node.type == MarkdownTokenTypes.WHITE_SPACE) {
            return text   // do not trim trailing whitespace
        }

        var currentCodeFenceLang = "tolk"

        val sb = StringBuilder()
        visit { node, processChildren ->
            fun wrapChildren(tag: String, newline: Boolean = false) {
                sb.append("<$tag>")
                processChildren()
                sb.append("</$tag>")
                if (newline) sb.appendLine()
            }

            val nodeType = node.type
            val nodeText = node.text
            when (nodeType) {
                MarkdownElementTypes.UNORDERED_LIST -> wrapChildren("ul", newline = true)
                MarkdownElementTypes.ORDERED_LIST -> wrapChildren("ol", newline = true)
                MarkdownElementTypes.LIST_ITEM -> wrapChildren("li")
                MarkdownElementTypes.EMPH -> wrapChildren("em")
                MarkdownElementTypes.STRONG -> wrapChildren("strong")
                GFMElementTypes.STRIKETHROUGH -> wrapChildren("del")
                MarkdownElementTypes.ATX_1 -> wrapChildren("h1")
                MarkdownElementTypes.ATX_2 -> wrapChildren("h2")
                MarkdownElementTypes.ATX_3 -> wrapChildren("h3")
                MarkdownElementTypes.ATX_4 -> wrapChildren("h4")
                MarkdownElementTypes.ATX_5 -> wrapChildren("h5")
                MarkdownElementTypes.ATX_6 -> wrapChildren("h6")
                MarkdownElementTypes.BLOCK_QUOTE -> wrapChildren("blockquote")
                MarkdownElementTypes.PARAGRAPH -> {
                    sb.trimEnd()
                    wrapChildren("p", newline = true)
                }

                MarkdownElementTypes.CODE_SPAN -> {
                    val startDelimiter = node.child(MarkdownTokenTypes.BACKTICK)?.text
                    if (startDelimiter != null) {
                        val text = node.text.substring(startDelimiter.length).removeSuffix(startDelimiter)
                        sb.append("<code style='font-size:${DocumentationSettings.getMonospaceFontSizeCorrection(true)}%;'>")
                        sb.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
                            DocumentationSettings.getInlineCodeHighlightingMode(),
                            owner.project,
                            TolkLanguage,
                            text
                        )
                        sb.append("</code>")
                    }
                }

                MarkdownElementTypes.CODE_BLOCK,
                MarkdownElementTypes.CODE_FENCE,
                    -> {
                    sb.trimEnd()
                    var language: Language = TolkLanguage
                    val contents = StringBuilder()
                    node.children.forEach { child ->
                        when (child.type) {
                            MarkdownTokenTypes.CODE_LINE, MarkdownTokenTypes.CODE_FENCE_CONTENT, MarkdownTokenTypes.EOL ->
                                contents.append(child.text)

                            MarkdownTokenTypes.FENCE_LANG -> {
                                language = guessLanguage(child.text.trim().split(' ')[0]) ?: language
                            }
                        }
                    }

                    sb.append("<pre><code>")
                    sb.appendHighlightedCode(
                        owner.project,
                        language,
                        DocumentationSettings.isHighlightingOfCodeBlocksEnabled(),
                        contents,
                        isForRenderedDoc = true,
                        trim = true
                    )
                    sb.append("</code></pre>")
                }

                MarkdownTokenTypes.FENCE_LANG -> {
                    currentCodeFenceLang = nodeText
                }

                MarkdownElementTypes.SHORT_REFERENCE_LINK,
                MarkdownElementTypes.FULL_REFERENCE_LINK,
                    -> {
                    val linkLabelNode = node.child(MarkdownElementTypes.LINK_LABEL)
                    val linkLabelContent = linkLabelNode?.children
                        ?.dropWhile { it.type == MarkdownTokenTypes.LBRACKET }
                        ?.dropLastWhile { it.type == MarkdownTokenTypes.RBRACKET }
                    if (linkLabelContent != null) {
                        val label = linkLabelContent.joinToString(separator = "") { it.text }
                        val linkText = node.child(MarkdownElementTypes.LINK_TEXT)?.toHtml() ?: label
                        if (owner is TolkFunction) {
                            val resolved = TolkDocumentationProvider.resolve(label, owner)
                            if (resolved != null) {
                                val hyperlink = buildString {
                                    DocumentationManagerUtil.createHyperlink(
                                        this,
                                        label,
                                        linkText,
                                        false,
                                        true
                                    )
                                }
                                sb.append(hyperlink)
                            } else {
                                sb.append(node.text)
                            }
                        }
                    } else {
                        sb.append(node.text)
                    }
                }

                MarkdownElementTypes.INLINE_LINK -> {
                    val label = node.child(MarkdownElementTypes.LINK_TEXT)?.toHtml()
                    val destination = node.child(MarkdownElementTypes.LINK_DESTINATION)?.text
                    if (label != null && destination != null) {
                        sb.append("<a href=\"$destination\">$label</a>")
                    } else {
                        sb.append(node.text)
                    }
                }

                MarkdownTokenTypes.TEXT,
                MarkdownTokenTypes.WHITE_SPACE,
                MarkdownTokenTypes.COLON,
                MarkdownTokenTypes.SINGLE_QUOTE,
                MarkdownTokenTypes.DOUBLE_QUOTE,
                MarkdownTokenTypes.LPAREN,
                MarkdownTokenTypes.RPAREN,
                MarkdownTokenTypes.LBRACKET,
                MarkdownTokenTypes.RBRACKET,
                MarkdownTokenTypes.EXCLAMATION_MARK,
                GFMTokenTypes.CHECK_BOX,
                GFMTokenTypes.GFM_AUTOLINK,
                    -> {
                    sb.append(nodeText)
                }

                MarkdownTokenTypes.CODE_LINE,
                MarkdownTokenTypes.CODE_FENCE_CONTENT,
                    -> {
                    sb.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
                        when (DocumentationSettings.isHighlightingOfCodeBlocksEnabled()) {
                            true -> DocumentationSettings.InlineCodeHighlightingMode.SEMANTIC_HIGHLIGHTING
                            false -> DocumentationSettings.InlineCodeHighlightingMode.NO_HIGHLIGHTING
                        },
                        owner.project,
                        guessLanguage(currentCodeFenceLang) ?: TolkLanguage,
                        nodeText
                    )
                }

                MarkdownTokenTypes.EOL -> {
                    val parentType = node.parent?.type
                    if (parentType == MarkdownElementTypes.CODE_BLOCK || parentType == MarkdownElementTypes.CODE_FENCE) {
                        sb.append("\n")
                    } else {
                        sb.append(" ")
                    }
                }

                MarkdownTokenTypes.GT -> sb.append("&gt;")
                MarkdownTokenTypes.LT -> sb.append("&lt;")

                MarkdownElementTypes.LINK_TEXT -> {
                    val childrenWithoutBrackets = node.children.drop(1).dropLast(1)
                    for (child in childrenWithoutBrackets) {
                        sb.append(child.toHtml())
                    }
                }

                MarkdownTokenTypes.EMPH -> {
                    val parentNodeType = node.parent?.type
                    if (parentNodeType != MarkdownElementTypes.EMPH && parentNodeType != MarkdownElementTypes.STRONG) {
                        sb.append(node.text)
                    }
                }

                GFMTokenTypes.TILDE -> {
                    if (node.parent?.type != GFMElementTypes.STRIKETHROUGH) {
                        sb.append(node.text)
                    }
                }

                GFMElementTypes.TABLE -> {
                    val alignment: List<String> = getTableAlignment(node)
                    var addedBody = false
                    sb.append("<table>")

                    for (child in node.children) {
                        if (child.type == GFMElementTypes.HEADER) {
                            sb.append("<thead>")
                            processTableRow(sb, child, "th", alignment)
                            sb.append("</thead>")
                        } else if (child.type == GFMElementTypes.ROW) {
                            if (!addedBody) {
                                sb.append("<tbody>")
                                addedBody = true
                            }

                            processTableRow(sb, child, "td", alignment)
                        }
                    }

                    if (addedBody) {
                        sb.append("</tbody>")
                    }
                    sb.append("</table>")
                }

                else -> {
                    processChildren()
                }
            }
        }
        return sb.toString().trimEnd()
    }
}

private fun processTableRow(sb: StringBuilder, node: MarkdownNode, cellTag: String, alignment: List<String>) {
    sb.append("<tr>")
    for ((i, child) in node.children.filter { it.type == GFMTokenTypes.CELL }.withIndex()) {
        val alignValue = alignment.getOrElse(i) { "" }
        val alignTag = if (alignValue.isEmpty()) "" else " align=\"$alignValue\""
        sb.append("<$cellTag$alignTag>")
        sb.append(child.toHtml())
        sb.append("</$cellTag>")
    }
    sb.append("</tr>")
}

private fun getTableAlignment(node: MarkdownNode): List<String> {
    val separatorRow = node.child(GFMTokenTypes.TABLE_SEPARATOR)
        ?: return emptyList()

    return separatorRow.text.split('|').filterNot { it.isBlank() }.map {
        val trimmed = it.trim()
        val left = trimmed.startsWith(':')
        val right = trimmed.endsWith(':')
        if (left && right) "center"
        else if (right) "right"
        else if (left) "left"
        else ""
    }
}

private fun StringBuilder.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
    highlightingMode: DocumentationSettings.InlineCodeHighlightingMode,
    project: Project,
    language: Language,
    codeSnippet: String,
): StringBuilder {
    val codeSnippetBuilder = StringBuilder()
    if (highlightingMode == DocumentationSettings.InlineCodeHighlightingMode.SEMANTIC_HIGHLIGHTING) { // highlight code by lexer
        HtmlSyntaxInfoUtil.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
            codeSnippetBuilder,
            project,
            language,
            codeSnippet,
            false,
            DocumentationSettings.getHighlightingSaturation(true)
        )
    } else {
        codeSnippetBuilder.append(StringUtil.escapeXmlEntities(codeSnippet))
    }
    if (highlightingMode != DocumentationSettings.InlineCodeHighlightingMode.NO_HIGHLIGHTING) {
        // set code text color as editor default code color instead of doc component text color
        val codeAttributes =
            EditorColorsManager.getInstance().globalScheme.getAttributes(HighlighterColors.TEXT).clone()
        codeAttributes.backgroundColor = null
        appendStyledSpan(true, codeAttributes, codeSnippetBuilder.toString())
    } else {
        append(codeSnippetBuilder.toString())
    }
    return this
}

private fun StringBuilder.appendStyledSpan(
    doHighlighting: Boolean,
    attributes: TextAttributes,
    value: String?,
): StringBuilder {
    if (doHighlighting) {
        HtmlSyntaxInfoUtil.appendStyledSpan(
            this,
            attributes,
            value,
            DocumentationSettings.getHighlightingSaturation(true)
        )
    } else {
        append(value)
    }
    return this
}

private fun getTargetLinkElementAttributes(key: TextAttributesKey): TextAttributes {
    return tuneAttributesForLink(EditorColorsManager.getInstance().globalScheme.getAttributes(key))
}

private fun getTargetLinkElementAttributes(element: PsiElement?): TextAttributes {
    return TextAttributes().apply {
        foregroundColor =
            EditorColorsManager.getInstance().globalScheme.getColor(DefaultLanguageHighlighterColors.DOC_COMMENT_LINK)
    }
}

private fun tuneAttributesForLink(attributes: TextAttributes): TextAttributes {
    val globalScheme = EditorColorsManager.getInstance().globalScheme
    if (attributes.foregroundColor == globalScheme.getAttributes(HighlighterColors.TEXT).foregroundColor
        || attributes.foregroundColor == globalScheme.getAttributes(DefaultLanguageHighlighterColors.IDENTIFIER).foregroundColor
    ) {
        val tuned = attributes.clone()
        if (ApplicationManager.getApplication().isUnitTestMode) {
            tuned.foregroundColor = globalScheme.getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES).foregroundColor
        } else {
            tuned.foregroundColor = globalScheme.getColor(DefaultLanguageHighlighterColors.DOC_COMMENT_LINK)
        }
        return tuned
    }
    return attributes
}

private fun guessLanguage(language: String?): Language? =
    if (language == null)
        null
    else
        Language
            .findInstancesByMimeType(language)
            .asSequence()
            .plus(Language.findInstancesByMimeType("text/$language"))
            .plus(
                Language.getRegisteredLanguages()
                    .asSequence()
                    .filter { languageMatches(language, it) }
            )
            .firstOrNull()

private fun languageMatches(langType: String, language: Language): Boolean =
    langType.equals(language.id, ignoreCase = true)
            || FileTypeManager.getInstance().getFileTypeByExtension(langType) === language.associatedFileType

private fun StringBuilder.appendHighlightedCode(
    project: Project, language: Language?, doHighlighting: Boolean,
    code: CharSequence, isForRenderedDoc: Boolean, trim: Boolean
): StringBuilder {
    val processedCode = code.toString().trim('\n', '\r').replace(' ', ' ')
        .applyIf(trim) { trimEnd() }
    if (language != null && doHighlighting) {
        HtmlSyntaxInfoUtil.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
            this, project, language, processedCode,
            trim, DocumentationSettings.getHighlightingSaturation(isForRenderedDoc)
        )
    } else {
        append(XmlStringUtil.escapeString(processedCode.applyIf(trim) { trimIndent() }))
    }
    return this
}
