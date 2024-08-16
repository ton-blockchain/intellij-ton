package org.ton.intellij.tolk.ide.quickdoc

import com.intellij.codeInsight.navigation.targetPresentation
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.model.Pointer
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.elementType
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.highlighting.TolkColor
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.isImpure
import org.ton.intellij.tolk.psi.impl.isMutable
import java.util.*

class TolkPsiDocumentationTargetProvider : PsiDocumentationTargetProvider {
    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        return if (element.language == TolkLanguage) {
            TolkDocumentationTarget(element, originalElement)
        } else null
    }
}

private const val NBSP = "&nbsp;"

@Suppress("UnstableApiUsage")
class TolkDocumentationTarget(val element: PsiElement, val originalElement: PsiElement?) : DocumentationTarget {
    override fun computePresentation(): TargetPresentation =
        targetPresentation(element)

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPtr = element.createSmartPointer()
        val originalElementPtr = originalElement?.createSmartPointer()
        return Pointer {
            val element = elementPtr.dereference() ?: return@Pointer null
            TolkDocumentationTarget(element, originalElementPtr?.dereference())
        }
    }

    override fun computeDocumentationHint(): String? =
        computeLocalDocumentation(element, originalElement, true)

    override fun computeDocumentation(): DocumentationResult? =
        computeLocalDocumentation(element, originalElement, false)?.let {
            DocumentationResult.documentation(it)
        }

    override val navigatable: Navigatable?
        get() = element as? Navigatable

    fun <E : PsiElement> E.createSmartPointer(): SmartPsiElementPointer<E> =
        SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this)

    private fun computeLocalDocumentation(
        element: PsiElement,
        originalElement: PsiElement?,
        quickNavigation: Boolean,
    ): String? {
        val (resolvedElement, elementHtml) = renderElement(element, originalElement) ?: return null
        return buildString {
            append(elementHtml)

            val doc = findDocumentationComment(resolvedElement)
            if (doc.isNotBlank()) {
                append(DocumentationMarkup.CONTENT_START)
                append(doc)
                append(DocumentationMarkup.CONTENT_END)
            }
        }
    }

    fun findDocumentationComment(
        originalElement: PsiElement,
    ): String {
        if (originalElement !is TolkElement) return ""
        val result = LinkedList<String>()
        var element = originalElement.prevSibling
        while (element.elementType in FUNC_DOC_COMMENTS || element is PsiWhiteSpace) {
            if (element.elementType in FUNC_DOC_COMMENTS) {
                val commentText = element.text.replaceFirst("[!; ]+".toRegex(), "")
                result.add(commentText)
            }
            element = element.prevSibling
        }
        val comment = result.asReversed().joinToString("\n ")
        val markdownTree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(comment)
        val markdownNode = MarkdownNode(markdownTree, null, comment, originalElement)
        return markdownNode.toHtml()
    }

    fun renderElement(
        element: PsiElement,
        originalElement: PsiElement?,
    ): Pair<PsiElement, String>? = element to buildString {
        when (element) {
            is TolkReferenceExpression -> {
//                val resolved = element.reference?.resolve()
//                if (resolved != null) {
//                    return renderElement(resolved, originalElement)
//                } else {
//                    val varExpr = PsiTreeUtil.getParentOfType(element, TolkVarExpression::class.java) ?: return null
//                    val left = varExpr.expressionList[0]
//                    val right = varExpr.expressionList[1]
//                    when (left) {
//                        is TolkPrimitiveTypeExpression -> renderType(left.primitiveType)
//                        is TolkHoleTypeExpression -> renderType(left.holeType)
//                        else -> append(left.text)
//                    }
//                    append(NBSP)
//                    append(right.text)
//                }
            }

            is TolkFunction -> {
                append(DocumentationMarkup.DEFINITION_START)
                renderFunction(element)
                append(DocumentationMarkup.DEFINITION_END)
            }

            is TolkFunctionParameter -> {
                append(DocumentationMarkup.DEFINITION_START)
                renderFunctionParameter(element)
                append(DocumentationMarkup.DEFINITION_END)
            }

            is TolkTypeParameter -> {
                append(DocumentationMarkup.DEFINITION_START)
                renderTypeParameter(element)
                append(DocumentationMarkup.DEFINITION_END)
            }

            else -> element.toString()
        }
    }

    fun StringBuilder.renderFunction(
        function: TolkFunction,
    ) {
        val typeParameterList = function.typeParameterList
        if (typeParameterList.isNotEmpty()) {
            appendStyledSpan(TolkColor.KEYWORD.attributes, "forall")
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
        appendStyledSpan(TolkColor.FUNCTION_DECLARATION.attributes, function.name)
        appendStyledSpan(TolkColor.PARENTHESES.attributes, "(")
        function.functionParameterList.joinTo(this) { param ->
            buildString {
                renderFunctionParameter(param)
            }
        }
        appendStyledSpan(TolkColor.PARENTHESES.attributes, ")")
        if (function.isImpure) {
            append(NBSP)
            appendStyledSpan(TolkColor.KEYWORD.attributes, "impure")
        }
    }

    fun StringBuilder.renderFunctionParameter(
        param: TolkFunctionParameter,
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

    fun StringBuilder.renderTypeParameter(
        typeParameter: TolkTypeParameter,
    ) {
        appendStyledSpan(TolkColor.TYPE_PARAMETER.attributes, typeParameter.name)
    }

    fun StringBuilder.renderType(
        type: TolkTypeReference,
    ) {
        when (type) {
            is TolkTypeIdentifier ->
                appendStyledSpan(TolkColor.TYPE_PARAMETER.attributes, type.identifier.text)

            is TolkPrimitiveType ->
                appendStyledSpan(TolkColor.KEYWORD.attributes, type.text)

            is TolkTupleType -> {
                appendStyledSpan(TolkColor.BRACKETS.attributes, "[")
                type.typeReferenceList.joinTo(this) {
                    buildString {
                        renderType(it)
                    }
                }
                appendStyledSpan(TolkColor.BRACKETS.attributes, "]")

            }

            is TolkTensorType -> {
                appendStyledSpan(TolkColor.PARENTHESES.attributes, "(")
                type.typeReferenceList.joinTo(this) {
                    buildString {
                        renderType(it)
                    }
                }
                appendStyledSpan(TolkColor.PARENTHESES.attributes, ")")
            }

            is TolkHoleType -> {
                if (type.text == "var") {
                    appendStyledSpan(TolkColor.KEYWORD.attributes, "var")
                } else {
                    append("_")
                }
            }

            else -> append(type)
        }
    }

    fun StringBuilder.appendStyledSpan(
        attributes: TextAttributes,
        value: String?,
    ) {
        HtmlSyntaxInfoUtil.appendStyledSpan(this, attributes, value, 1.0f)
    }
}
