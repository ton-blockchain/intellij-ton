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
import org.ton.intellij.tolk.doc.DocumentationUtils.asConstant
import org.ton.intellij.tolk.doc.DocumentationUtils.asDot
import org.ton.intellij.tolk.doc.DocumentationUtils.asFunction
import org.ton.intellij.tolk.doc.DocumentationUtils.asGlobalVariable
import org.ton.intellij.tolk.doc.DocumentationUtils.asIdentifier
import org.ton.intellij.tolk.doc.DocumentationUtils.asKeyword
import org.ton.intellij.tolk.doc.DocumentationUtils.asNumber
import org.ton.intellij.tolk.doc.DocumentationUtils.asParameter
import org.ton.intellij.tolk.doc.DocumentationUtils.asParen
import org.ton.intellij.tolk.doc.DocumentationUtils.asPrimitive
import org.ton.intellij.tolk.doc.DocumentationUtils.asString
import org.ton.intellij.tolk.doc.DocumentationUtils.asStruct
import org.ton.intellij.tolk.doc.DocumentationUtils.asTypeAlias
import org.ton.intellij.tolk.doc.DocumentationUtils.asTypeParameter
import org.ton.intellij.tolk.doc.DocumentationUtils.colorize
import org.ton.intellij.tolk.doc.DocumentationUtils.line
import org.ton.intellij.tolk.doc.DocumentationUtils.part
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.highlighting.TolkSyntaxHighlighter
import org.ton.intellij.tolk.psi.TOLK_KEYWORDS
import org.ton.intellij.tolk.psi.TOLK_NUMBERS
import org.ton.intellij.tolk.psi.TOLK_STRING_LITERALS
import org.ton.intellij.tolk.psi.TolkAnnotation
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.psi.TolkDocOwner
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkGlobalVar
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.tolk.psi.TolkSelfParameter
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypeExpression
import org.ton.intellij.tolk.psi.TolkTypeParameter
import org.ton.intellij.tolk.psi.TolkTypeParameterList
import org.ton.intellij.tolk.psi.impl.hasReceiver
import org.ton.intellij.tolk.psi.impl.receiverTy
import org.ton.intellij.tolk.psi.impl.returnTy
import org.ton.intellij.tolk.type.TolkIntTyFamily
import org.ton.intellij.tolk.type.TolkPrimitiveTy
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyAlias
import org.ton.intellij.tolk.type.TolkTyStruct
import org.ton.intellij.tolk.type.TolkTyUnknown
import org.ton.intellij.tolk.type.render
import org.ton.intellij.util.parentOfType
import java.util.function.Consumer
import kotlin.math.max

class TolkDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?) = when (element) {
        is TolkFunction    -> element.generateDoc()
        is TolkConstVar    -> element.generateDoc()
        is TolkGlobalVar   -> element.generateDoc()
        is TolkTypeDef     -> element.generateDoc()
        is TolkStruct      -> element.generateDoc()
        is TolkStructField -> element.generateDoc()
        else               -> null
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

fun TolkConstVar.generateDoc(): String {
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        line(annotations.annotations().generateDoc())

        part("const", asKeyword)
        colorize(name ?: "", asConstant)

        val type = type ?: TolkTyUnknown
        // always render type even it omitted in code
        append(": ")
        append(type.generateDoc())

        val expr = expression
        if (expr != null) {
            append(" = ")
            append(expr.generateDoc())
        }

        append(DocumentationMarkup.DEFINITION_END)
        generateCommentsPart(this@generateDoc)
    }
}

fun TolkGlobalVar.generateDoc(): String {
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        line(annotations.annotations().generateDoc())

        part("global", asKeyword)
        colorize(name ?: "", asGlobalVariable)

        val type = type ?: TolkTyUnknown
        append(": ")
        append(type.generateDoc())

        append(DocumentationMarkup.DEFINITION_END)
        generateCommentsPart(this@generateDoc)
    }
}

fun TolkTypeDef.generateDoc(): String {
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        line(annotations.annotations().generateDoc())

        part("type", asKeyword)
        colorize(name ?: "", asTypeAlias)

        val typeParams = typeParameterList
        if (typeParams != null) {
            append(typeParams.generateDoc())
        }

        append(" = ")

        val primitiveType = TolkPrimitiveTy.fromName(name ?: "")
        if (primitiveType != null || typeExpression?.text == "builtin") {
            part("builtin", asKeyword)
        } else {
            val type = type as? TolkTyAlias
            if (type != null && type.underlyingType !is TolkTyUnknown) {
                append(type.underlyingType.generateDoc())
            } else {
                colorize(typeExpression?.text ?: "unknown", asIdentifier)
            }
        }

        append(DocumentationMarkup.DEFINITION_END)
        generateCommentsPart(this@generateDoc)
    }
}

fun TolkStruct.generateDoc(): String {
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        line(annotations.annotations().generateDoc())

        part("struct", asKeyword)

        val tag = structConstructorTag
        if (tag != null) {
            colorize("(", asParen)
            val tagValue = tag.integerLiteral?.text
            if (tagValue != null) {
                colorize(tagValue, asNumber)
            }
            colorize(")", asParen)
            append(" ")
        }

        colorize(name ?: "", asStruct)

        val typeParams = typeParameterList
        if (typeParams != null) {
            append(typeParams.generateDoc())
        }

        generateStructFields(structBody?.structFieldList ?: emptyList())

        append(DocumentationMarkup.DEFINITION_END)
        generateCommentsPart(this@generateDoc)
    }
}

fun TolkStructField.generateDoc(): String {
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        val owner = parentOfType<TolkStruct>()
        if (owner != null && owner.name != null) {
            colorize("struct", asKeyword)
            append(" ")
            colorize(owner.name ?: "", asStruct)
            append("\n")
        }

        colorize(name ?: "", asParameter)
        append(": ")

        val type = type
        if (type != null) {
            append(type.generateDoc())
        }

        val defaultValue = expression
        if (defaultValue != null) {
            append(" = ")
            append(defaultValue.generateDoc())
        }

        append(DocumentationMarkup.DEFINITION_END)
        generateCommentsPart(this@generateDoc)
    }
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

private fun StringBuilder.generateStructFields(fields: List<TolkStructField>) {
    if (fields.isEmpty()) {
        colorize(" {}", asParen)
        return
    }

    colorize(" {", asParen)
    appendLine()
    append(
        fields.joinToString("\n") { field ->
            buildString {
                append("   ")
                colorize(field.name ?: "", asParameter)
                append(": ")

                val type = field.type
                if (type != null) {
                    append(type.generateDoc())
                }

                val defaultValue = field.expression
                if (defaultValue != null) {
                    append(" = ")
                    append(defaultValue.generateDoc())
                }
            }
        }
    )
    append("\n")
    colorize("}", asParen)
}

fun TolkTypeParameterList.generateDoc(): String {
    val params = typeParameterList
    if (params.isEmpty()) return ""

    return buildString {
        colorize("<", asParen)
        params.forEachIndexed { index, param ->
            if (index > 0) {
                colorize(", ", asComma)
            }
            colorize(param.name ?: "", asTypeParameter)
            val defaultType = param.defaultTypeParameter?.typeExpression?.type
            if (defaultType != null) {
                append(" = ")
                append(defaultType.generateDoc())
            }
        }
        colorize(">", asParen)
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
