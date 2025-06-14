package org.ton.intellij.tolk.doc

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.psi.*
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.ide.colors.TolkColor
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.isMutable
import org.ton.intellij.util.markdown.MarkdownDocAstBuilder
import java.util.function.Consumer

class TolkDocumentationProvider : AbstractDocumentationProvider() {

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
//        val renderedElement = renderElement(element, originalElement) ?: return null

        return buildString {
//            append(DocumentationMarkup.DEFINITION_START)
//            append(renderedElement)
//            append(DocumentationMarkup.DEFINITION_END)
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
        if (file !is TolkFile) return
        SyntaxTraverser.psiTraverser(file).forEach {
            if (it is TolkDocComment) {
                sink.accept(it)
            }
        }
    }

    override fun generateRenderedDoc(comment: PsiDocCommentBase): String? {
        return (comment as? TolkDocComment)?.renderHtml()
    }

    private fun getComments(element: PsiElement?): PsiComment? {
        return (element as? TolkFunction)?.firstChild as? PsiComment
    }

    private fun getCommentText(comment: PsiComment): String {
        return (comment as? TolkDocComment)?.renderHtml()
            ?: MarkdownDocAstBuilder.renderHtml(comment.node.chars, "///", TolkDocMarkdownFlavourDescriptor())
    }

    fun renderElement(element: PsiElement?, context: PsiElement?): String? {
        return when (element) {
            null -> null
            is TolkFunction -> buildString {
                renderFunction(element)
            }

            else -> null
        }
    }

    fun StringBuilder.renderFunction(
        function: TolkFunction,
    ) {
        val typeParameterList = function.typeParameterList?.typeParameterList
        if (typeParameterList.isNullOrEmpty()) {
            appendStyledSpan(TolkColor.KEYWORD.attributes, "forall")
            append(NBSP)
            typeParameterList?.joinTo(this) {
                buildString {
                    renderTypeParameter(it)
                }
            }
            append(NBSP)
            append("->")
            append(NBSP)
        }
//        function.typeReference?.let {
//            renderType(it)
//        }
        append(NBSP)
        if (function.isMutable) {
            append("~")
        }
        appendStyledSpan(TolkColor.FUNCTION.attributes, function.name)
        appendStyledSpan(TolkColor.PARENTHESES.attributes, "(")
        function.parameterList?.parameterList?.joinTo(this) { param ->
            buildString {
//                renderFunctionParameter(param)
            }
        }
        appendStyledSpan(TolkColor.PARENTHESES.attributes, ")")
    }

//    private fun StringBuilder.renderFunctionParameter(
//        param: TolkParameter,
//    ) {
//        val type = param.typeElement
//        if (type != null) {
//            renderType(type)
//        }
//        val name = param.name
//        if (name != null) {
//            if (type != null) {
//                append(NBSP)
//            }
//            append(name)
//        }
//    }

    private fun StringBuilder.renderTypeParameter(
        typeParameter: TolkTypeParameter,
    ) {
        appendStyledSpan(TolkColor.TYPE_PARAMETER.attributes, typeParameter.name)
    }

    private fun StringBuilder.renderType(
        type: TolkTypeExpression,
    ) {
        when (type) {
//            is TolkTypeIdentifier ->
//                if (type.isPrimitive) {
//                    appendStyledSpan(TolkColor.KEYWORD.attributes, type.text)
//                } else {
//                    appendStyledSpan(TolkColor.TYPE_PARAMETER.attributes, type.identifier.text)
//                }

            is TolkTupleTypeExpression -> {
//                appendStyledSpan(TolkColor.BRACKETS.attributes, "[")
//                type.typeReferenceList.joinTo(this) {
//                    buildString {
//                        renderType(it)
//                    }
//                }
//                appendStyledSpan(TolkColor.BRACKETS.attributes, "]")

            }

            is TolkTensorTypeExpression -> {
//                appendStyledSpan(TolkColor.PARENTHESES.attributes, "(")
//                type.typeReferenceList.joinTo(this) {
//                    buildString {
//                        renderType(it)
//                    }
//                }
//                appendStyledSpan(TolkColor.PARENTHESES.attributes, ")")
            }

            is TolkParenTypeExpression -> {
//                appendStyledSpan(TolkColor.PARENTHESES.attributes, "(")
//                type.typeReference?.let {
//                    renderType(it)
//                }
//                appendStyledSpan(TolkColor.PARENTHESES.attributes, ")")
            }

            else -> {
                append(type)
            }
        }
    }

    companion object {
        fun resolve(link: String?, context: PsiElement?): PsiElement? {
            if (link == null) return null
            if (context is TolkFunction) {
                var resolved: PsiElement? =
                    context.parameterList?.parameterList?.find {
                        it.name == link
                    }
                if (resolved == null) {
                    resolved = context.typeParameterList?.typeParameterList?.find {
                        it.name == link
                    }
                }
                if (resolved == null) {
                    val file = context.containingFile as? TolkFile
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
