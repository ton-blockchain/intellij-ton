package org.ton.intellij.func.doc

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.psi.*
import org.ton.intellij.func.doc.psi.FuncDocComment
import org.ton.intellij.func.highlighting.FuncColor
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.isImpure
import org.ton.intellij.func.psi.impl.isMutable
import org.ton.intellij.markdown.MarkdownDocAstBuilder
import java.util.function.Consumer

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

        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            append(renderedElement)
            append(DocumentationMarkup.DEFINITION_END)
            val doc = getComments(element)
            if (doc != null) {
                append(DocumentationMarkup.CONTENT_START)
                append(getCommentText(doc))
                append(DocumentationMarkup.CONTENT_END)
            }
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
        SyntaxTraverser.psiTraverser(file).forEach {
            if (it is FuncDocComment) {
                sink.accept(it)
            }
        }
    }

    override fun generateRenderedDoc(comment: PsiDocCommentBase): String? {
        return (comment as? FuncDocComment)?.renderHtml()
    }

    private fun getComments(element: PsiElement?): PsiComment? {
        return (element as? FuncFunction)?.firstChild as? PsiComment
    }

    private fun getCommentText(comment: PsiComment): String {
        return (comment as? FuncDocComment)?.renderHtml()
            ?: MarkdownDocAstBuilder.renderHtml(comment.node.chars, ";;;", FuncDocMarkdownFlavourDescriptor())
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
            appendStyledSpan(FuncColor.KEYWORD.attributes, "forall")
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
        renderType(function.typeReference)
        append(NBSP)
        if (function.isMutable) {
            append("~")
        }
        appendStyledSpan(FuncColor.FUNCTION_DECLARATION.attributes, function.name)
        appendStyledSpan(FuncColor.PARENTHESES.attributes, "(")
        function.functionParameterList.joinTo(this) { param ->
            buildString {
                renderFunctionParameter(param)
            }
        }
        appendStyledSpan(FuncColor.PARENTHESES.attributes, ")")
        if (function.isImpure) {
            append(NBSP)
            appendStyledSpan(FuncColor.KEYWORD.attributes, "impure")
        }
    }

    private fun StringBuilder.renderFunctionParameter(
        param: FuncFunctionParameter,
    ) {
        val type = param.typeReference
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
        appendStyledSpan(FuncColor.TYPE_PARAMETER.attributes, typeParameter.name)
    }

    private fun StringBuilder.renderType(
        type: FuncTypeReference,
    ) {
        when (type) {
            is FuncTypeIdentifier ->
                appendStyledSpan(FuncColor.TYPE_PARAMETER.attributes, type.identifier.text)

            is FuncPrimitiveType ->
                appendStyledSpan(FuncColor.KEYWORD.attributes, type.text)

            is FuncTupleType -> {
                appendStyledSpan(FuncColor.BRACKETS.attributes, "[")
                type.typeReferenceList.joinTo(this) {
                    buildString {
                        renderType(it)
                    }
                }
                appendStyledSpan(FuncColor.BRACKETS.attributes, "]")

            }

            is FuncTensorType -> {
                appendStyledSpan(FuncColor.PARENTHESES.attributes, "(")
                type.typeReferenceList.joinTo(this) {
                    buildString {
                        renderType(it)
                    }
                }
                appendStyledSpan(FuncColor.PARENTHESES.attributes, ")")
            }

            is FuncHoleType -> {
                if (type.text == "var") {
                    appendStyledSpan(FuncColor.KEYWORD.attributes, "var")
                } else {
                    append("_")
                }
            }

            is FuncParenType -> {
                appendStyledSpan(FuncColor.PARENTHESES.attributes, "(")
                type.typeReference?.let {
                    renderType(it)
                }
                appendStyledSpan(FuncColor.PARENTHESES.attributes, ")")
            }

            is FuncMapType -> {
                type.from?.let {
                    renderType(it)
                }
                append(" ")
                appendStyledSpan(FuncColor.OPERATION_SIGN.attributes, "->")
                append(" ")
                type.to?.let {
                    renderType(it)
                }
            }

            is FuncUnitType -> appendStyledSpan(FuncColor.PARENTHESES.attributes, "()")

            else -> {
                val typeIdentifier = type.typeIdentifier?.text
                if (typeIdentifier != null) {
                    appendStyledSpan(FuncColor.TYPE_PARAMETER.attributes, typeIdentifier)
                } else {
                    append(type)
                }
            }
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

private const val NBSP = "&nbsp;"

private fun StringBuilder.appendStyledSpan(
    attributes: TextAttributes,
    value: String?,
) {
    HtmlSyntaxInfoUtil.appendStyledSpan(this, attributes, value, 1.0f)
}
