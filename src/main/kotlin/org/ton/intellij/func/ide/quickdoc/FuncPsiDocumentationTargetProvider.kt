package org.ton.intellij.func.ide.quickdoc

import com.intellij.codeInsight.navigation.targetPresentation
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.model.Pointer
import com.intellij.navigation.TargetPresentation
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.elementType
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.highlighting.FuncColor
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.isImpure
import org.ton.intellij.func.psi.impl.isMutable
import java.util.*

class FuncPsiDocumentationTargetProvider : PsiDocumentationTargetProvider {
    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        return if (element.language == FuncLanguage) {
            FuncDocumentationTarget(element, originalElement)
        } else null
    }
}

private const val NBSP = "&nbsp;"

@Suppress("UnstableApiUsage")
class FuncDocumentationTarget(val element: PsiElement, val originalElement: PsiElement?) : DocumentationTarget {
    override fun computePresentation(): TargetPresentation =
        targetPresentation(element)

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPtr = element.createSmartPointer()
        val originalElementPtr = originalElement?.createSmartPointer()
        return Pointer {
            val element = elementPtr.dereference() ?: return@Pointer null
            FuncDocumentationTarget(element, originalElementPtr?.dereference())
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
        if (originalElement !is FuncElement) return ""
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
            is FuncReferenceExpression -> {
//                val resolved = element.reference?.resolve()
//                if (resolved != null) {
//                    return renderElement(resolved, originalElement)
//                } else {
//                    val varExpr = PsiTreeUtil.getParentOfType(element, FuncVarExpression::class.java) ?: return null
//                    val left = varExpr.expressionList[0]
//                    val right = varExpr.expressionList[1]
//                    when (left) {
//                        is FuncPrimitiveTypeExpression -> renderType(left.primitiveType)
//                        is FuncHoleTypeExpression -> renderType(left.holeType)
//                        else -> append(left.text)
//                    }
//                    append(NBSP)
//                    append(right.text)
//                }
            }

            is FuncFunction -> {
                append(DocumentationMarkup.DEFINITION_START)
                renderFunction(element)
                append(DocumentationMarkup.DEFINITION_END)
            }

            is FuncFunctionParameter -> {
                append(DocumentationMarkup.DEFINITION_START)
                renderFunctionParameter(element)
                append(DocumentationMarkup.DEFINITION_END)
            }

            is FuncTypeParameter -> {
                append(DocumentationMarkup.DEFINITION_START)
                renderTypeParameter(element)
                append(DocumentationMarkup.DEFINITION_END)
            }

            else -> element.toString()
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

    fun StringBuilder.renderFunctionParameter(
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

    fun StringBuilder.renderTypeParameter(
        typeParameter: FuncTypeParameter,
    ) {
        appendStyledSpan(FuncColor.TYPE_PARAMETER.attributes, typeParameter.name)
    }

    fun StringBuilder.renderType(
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
