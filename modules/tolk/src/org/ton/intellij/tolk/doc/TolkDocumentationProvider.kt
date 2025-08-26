package org.ton.intellij.tolk.doc

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import org.ton.intellij.tolk.doc.DocumentationUtils.appendNotNull
import org.ton.intellij.tolk.doc.DocumentationUtils.asAnnotation
import org.ton.intellij.tolk.doc.DocumentationUtils.asComma
import org.ton.intellij.tolk.doc.DocumentationUtils.asDot
import org.ton.intellij.tolk.doc.DocumentationUtils.asFunction
import org.ton.intellij.tolk.doc.DocumentationUtils.asKeyword
import org.ton.intellij.tolk.doc.DocumentationUtils.asNumber
import org.ton.intellij.tolk.doc.DocumentationUtils.asParameter
import org.ton.intellij.tolk.doc.DocumentationUtils.asParen
import org.ton.intellij.tolk.doc.DocumentationUtils.asPrimitive
import org.ton.intellij.tolk.doc.DocumentationUtils.asString
import org.ton.intellij.tolk.doc.DocumentationUtils.asStruct
import org.ton.intellij.tolk.doc.DocumentationUtils.asTypeAlias
import org.ton.intellij.tolk.doc.DocumentationUtils.colorize
import org.ton.intellij.tolk.doc.DocumentationUtils.line
import org.ton.intellij.tolk.doc.DocumentationUtils.part
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.highlighting.TolkSyntaxHighlighter
import org.ton.intellij.tolk.psi.TOLK_KEYWORDS
import org.ton.intellij.tolk.psi.TOLK_NUMBERS
import org.ton.intellij.tolk.psi.TOLK_STRING_LITERALS
import org.ton.intellij.tolk.psi.TolkAnnotation
import org.ton.intellij.tolk.psi.TolkDocOwner
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.tolk.psi.TolkSelfParameter
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.impl.hasReceiver
import org.ton.intellij.tolk.psi.impl.receiverTy
import org.ton.intellij.tolk.psi.impl.returnTy
import org.ton.intellij.tolk.type.TolkIntTyFamily
import org.ton.intellij.tolk.type.TolkPrimitiveTy
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyStruct
import org.ton.intellij.tolk.type.render
import java.util.function.Consumer
import kotlin.math.max

class TolkDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?) = when (element) {
        is TolkFunction -> element.generateDoc()
        else            -> null
    }

    override fun collectDocComments(file: PsiFile, sink: Consumer<in PsiDocCommentBase>) {
        if (file !is TolkFile) return
        for (element in SyntaxTraverser.psiTraverser(file)) {
            if (element is TolkDocComment && element.owner != null && element.text.startsWith("///")) {
                sink.accept(element)
            }
        }
    }

    override fun generateRenderedDoc(comment: PsiDocCommentBase): String? {
        return (comment as? TolkDocComment)?.renderHtml()
    }
}

fun TolkTy.generateDoc(): String {
    when (this) {
        is TolkIntTyFamily -> return buildString { colorize(render(), asPrimitive) }
        is TolkPrimitiveTy -> return buildString { colorize(render(), asPrimitive) }
        is TolkTyStruct    -> return buildString { colorize(render(), asStruct) }
        is TolkTypeDef     -> return buildString { colorize(render(), asTypeAlias) }
    }
    return render()
}

fun TolkFunction.generateDoc(): String {
    val parameters = parameterList?.parameterList ?: emptyList()
    val returnType = returnTy

    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        line(annotations.annotations().generateDoc())

        part("fun", asKeyword)

        val receiverTy = receiverTy
        if (hasReceiver) {
            appendNotNull(receiverTy.generateDoc())
            colorize(".", asDot)
        }

        colorize(name ?: "", asFunction)

        append(parameters.generateDoc(parameterList?.selfParameter))
        append(": ")
        appendNotNull(returnType.generateDoc())

        append(DocumentationMarkup.DEFINITION_END)
        generateCommentsPart(this@generateDoc)
    }
}

fun Sequence<TolkAnnotation>.generateDoc(): String {
    return joinToString("\n") { attr ->
        attr.generateDoc()
    }
}

fun TolkAnnotation.generateDoc(): String {
    val name = identifier?.text ?: ""
    val arguments = argumentList?.argumentList ?: emptyList()
    return buildString {
        colorize("@", asAnnotation)
        colorize(name, asAnnotation)
        if (arguments.isNotEmpty()) {
            colorize("(", asParen)
            arguments.forEachIndexed { index, argument ->
                append(argument.expression.generateDoc())
                if (index != arguments.size - 1) {
                    append(", ")
                }
            }
            colorize(")", asParen)
        }
    }
}

private fun List<TolkParameter>.generateDoc(selfParameter: TolkSelfParameter?): String {
    val params = this

    if (params.isEmpty()) {
        return buildString { colorize("()", asParen) }
    }

    fun StringBuilder.renderSelfParameter() {
        if (selfParameter != null) {
            if (selfParameter.isMutable) {
                colorize("mutate ", asKeyword)
            }
            colorize("self", asKeyword)
            colorize(", ", asComma)
        }
    }

    if (params.size == 1) {
        val param = params.first()
        return buildString {
            colorize("(", asParen)
            renderSelfParameter()
            append(param.generateDocForMethod())
            colorize(")", asParen)
        }
    }

    val paramNameMaxWidth = params.maxOfOrNull { it.name?.length ?: 0 } ?: 0

    return buildString {
        colorize("(", asParen)
        append("\n")
        if (selfParameter != null) {
            append("   ")
        }
        renderSelfParameter()
        if (selfParameter != null) {
            append("\n")
        }
        append(
            params.joinToString(",\n") { param ->
                buildString {
                    append("   ")

                    val name = param.name
                    if (name != null) {
                        colorize(name, asParameter)
                    }
                    append(": ")
                    val nameLength = name?.length ?: 0
                    append("".padEnd(max(paramNameMaxWidth - nameLength, 0)))
                    append(param.type?.generateDoc())
                }
            } + ","
        )
        append("\n")
        colorize(")", asParen)
    }
}

private fun TolkParameter.generateDocForMethod(): String {
    return buildString {
        val name = name
        if (name != null) {
            colorize(name, asParameter)
            append(": ")
        }
        append(type?.generateDoc())
    }
}

fun StringBuilder.generateCommentsPart(element: TolkDocOwner?) {
    val commentsList = listOf(element?.doc)
    if (commentsList.any { it is TolkDocComment }) {
        append(DocumentationMarkup.CONTENT_START)
        for (comment in commentsList) {
            if (comment is TolkDocComment) {
                append(comment.renderHtml())
                append("\n")
            }
        }
        append(DocumentationMarkup.CONTENT_END)
        return
    }
}

fun TolkExpression.generateDoc(): String {
    val text = text
    val highlighter = TolkSyntaxHighlighter()
    val lexer = highlighter.highlightingLexer
    val builder = StringBuilder()
    lexer.start(text)
    while (lexer.tokenType != null) {
        val type = lexer.tokenType
        val tokenText = lexer.tokenText
        val keyword = TOLK_KEYWORDS.contains(type)
        val number = TOLK_NUMBERS.contains(type)
        val string = TOLK_STRING_LITERALS.contains(type)
        val booleanLiteral = tokenText == "true" || tokenText == "false"
        val builtinFunctions = tokenText == "ton" || tokenText == "address"
        val primitiveType = TolkPrimitiveTy.fromName(tokenText)

        if (tokenText.contains("\n")) {
            builder.append("...")
            break
        }

        builder.append(
            when {
                keyword               -> colorize(tokenText, asKeyword)
                number                -> colorize(tokenText, asNumber)
                string                -> colorize(tokenText, asString)
                booleanLiteral        -> colorize(tokenText, asKeyword)
                primitiveType != null -> colorize(tokenText, asPrimitive)
                builtinFunctions      -> colorize(tokenText, asFunction)
                else                  -> tokenText
            }
        )
        lexer.advance()
    }

    return builder.toString()
}
