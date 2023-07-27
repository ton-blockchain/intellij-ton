package org.ton.intellij.func.ide

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.psi.*
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.ton.intellij.func.highlighting.FuncSyntaxHighlightingColors
import org.ton.intellij.func.ide.quickdoc.MarkdownNode
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.isImpure
import org.ton.intellij.func.psi.impl.isMutable
import java.util.*
import java.util.function.Consumer

private const val NBSP = "&nbsp;"

class FuncDocumentationProvider : AbstractDocumentationProvider() {

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) return null
        val renderedElement = renderElement(element, originalElement) ?: return null
        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            append(renderedElement)
            append(DocumentationMarkup.DEFINITION_END)
        }
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) return null
        val renderedElement = renderElement(element, originalElement) ?: return null
        val content = getCommentText(getComments(element), element)
        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            append(renderedElement)
            append(DocumentationMarkup.DEFINITION_END)
            append(DocumentationMarkup.CONTENT_START)
            append(content)
            append(DocumentationMarkup.CONTENT_END)
        }
    }

    override fun getDocumentationElementForLink(
        psiManager: PsiManager?,
        link: String?,
        context: PsiElement?,
    ): PsiElement? {
        return resolve(link, context)
    }

    override fun collectDocComments(file: PsiFile, sink: Consumer<in PsiDocCommentBase>) {
        if (file !is FuncFile) return
        file.functions.forEach {
//            val doc = it.doc
//            if (doc != null) {
//                println("collected doc: ${doc.text}")
//                sink.accept(doc)
//            }
        }
    }

    override fun generateRenderedDoc(comment: PsiDocCommentBase): String? {
        if (comment !is FuncDoc) return null
        return comment.text
    }

    private fun getComments(element: PsiElement?): List<PsiComment> {
        if (element == null) return emptyList()
        val result = LinkedList<PsiComment>()
        var e = element
        while (true) {
            e = e?.prevSibling
            when (e) {
                is PsiWhiteSpace -> {
                    if (e.text.contains("\n\n")) return result
                    continue
                }

                is PsiComment -> result.addFirst(e)
                else -> return result
            }
        }
    }

    private fun getCommentText(comments: List<PsiComment>, element: PsiElement): String {
        val rawText = comments.joinToString("\n") { it.text.replaceFirst("[!; ]+".toRegex(), "") }
        val markdownTree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(rawText)
        val markdownNode = MarkdownNode(markdownTree, null, rawText, element)
        return markdownNode.toHtml()
    }

    fun renderElement(element: PsiElement?, context: PsiElement?): String? {
        return when (element) {
            null -> null
            is FuncFunction -> buildString {
                renderFunction(element)
            }

            else -> null
        }
    }

    fun StringBuilder.renderFunction(
        function: FuncFunction,
    ) {
        val typeParameterList = function.typeParameterList
        if (typeParameterList.isNotEmpty()) {
            appendStyledSpan(FuncSyntaxHighlightingColors.KEYWORD.attributes, "forall")
            append(NBSP)
            typeParameterList.joinTo(this) {
                buildString {
                    renderTypeParameter(it)
                }
            }
            append(NBSP)
            append("->")
            append(NBSP)
        }
        renderType(function.type)
        append(NBSP)
        if (function.isMutable) {
            append("~")
        }
        appendStyledSpan(FuncSyntaxHighlightingColors.FUNCTION_DECLARATION.attributes, function.name)
        appendStyledSpan(FuncSyntaxHighlightingColors.PARENTHESES.attributes, "(")
        function.functionParameterList.joinTo(this) { param ->
            buildString {
                renderFunctionParameter(param)
            }
        }
        appendStyledSpan(FuncSyntaxHighlightingColors.PARENTHESES.attributes, ")")
        if (function.isImpure) {
            append(NBSP)
            appendStyledSpan(FuncSyntaxHighlightingColors.KEYWORD.attributes, "impure")
        }
    }

    private fun StringBuilder.renderFunctionParameter(
        param: FuncFunctionParameter,
    ) {
        val type = param.atomicType
        if (type != null) {
            renderType(type)
        }
        val name = param.name
        if (name != null) {
            if (type != null) {
                append(NBSP)
            }
            append(name)
        }
    }

    private fun StringBuilder.renderTypeParameter(
        typeParameter: FuncTypeParameter,
    ) {
        appendStyledSpan(FuncSyntaxHighlightingColors.TYPE_PARAMETER.attributes, typeParameter.name)
    }

    private fun StringBuilder.renderType(
        type: FuncType,
    ) {
        when (type) {
            is FuncTypeIdentifier ->
                appendStyledSpan(FuncSyntaxHighlightingColors.TYPE_PARAMETER.attributes, type.identifier.text)

            is FuncPrimitiveType ->
                appendStyledSpan(FuncSyntaxHighlightingColors.KEYWORD.attributes, type.text)

            is FuncTupleType -> {
                appendStyledSpan(FuncSyntaxHighlightingColors.BRACKETS.attributes, "[")
                type.tupleTypeItemList.joinTo(this) {
                    buildString {
                        renderType(it.type)
                    }
                }
                appendStyledSpan(FuncSyntaxHighlightingColors.BRACKETS.attributes, "]")

            }

            is FuncTensorType -> {
                appendStyledSpan(FuncSyntaxHighlightingColors.PARENTHESES.attributes, "(")
                type.typeList.joinTo(this) {
                    buildString {
                        renderType(it)
                    }
                }
                appendStyledSpan(FuncSyntaxHighlightingColors.PARENTHESES.attributes, ")")
            }

            is FuncHoleType -> {
                if (type.text == "var") {
                    appendStyledSpan(FuncSyntaxHighlightingColors.KEYWORD.attributes, "var")
                } else {
                    append("_")
                }
            }

            else -> append(type)
        }
    }

    companion object {
        fun resolve(link: String?, context: PsiElement?): PsiElement? {
            if (link == null) return null
            if (context is FuncFunction) {
                var resolved: PsiElement? =
                    context.functionParameterList.find {
                        it.name == link
                    }
                if (resolved == null) {
                    resolved = context.typeParameterList.find {
                        it.name == link
                    }
                }
                if (resolved == null) {
                    val file = context.containingFile as? FuncFile
                    resolved = file?.functions?.find {
                        it.name == link
                    }
                }
                return resolved
            }
            return null
        }
    }
}

private fun StringBuilder.appendStyledSpan(
    attributes: TextAttributes,
    value: String?,
) {
    HtmlSyntaxInfoUtil.appendStyledSpan(this, attributes, value, 1.0f)
}
