package org.ton.intellij.tolk.doc

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SyntaxTraverser
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.appendNotNull
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asAnnotation
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asComma
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asConstant
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asDot
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asEnum
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asEnumMember
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asField
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asFunction
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asGlobalVariable
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asIdentifier
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asKeyword
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asNumber
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asParameter
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asParen
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asPrimitive
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asString
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asStruct
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asTypeAlias
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.asTypeParameter
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.colorize
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.line
import org.ton.intellij.tolk.doc.TolkDocumentationUtils.part
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.highlighting.TolkSyntaxHighlighter
import org.ton.intellij.tolk.psi.TOLK_KEYWORDS
import org.ton.intellij.tolk.psi.TOLK_NUMBERS
import org.ton.intellij.tolk.psi.TOLK_STRING_LITERALS
import org.ton.intellij.tolk.psi.TolkAnnotation
import org.ton.intellij.tolk.psi.TolkCatchParameter
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.psi.TolkDocOwner
import org.ton.intellij.tolk.psi.TolkEnum
import org.ton.intellij.tolk.psi.TolkEnumMember
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkGlobalVar
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.tolk.psi.TolkSelfParameter
import org.ton.intellij.tolk.psi.TolkSelfTypeExpression
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypeParameter
import org.ton.intellij.tolk.psi.TolkTypeParameterList
import org.ton.intellij.tolk.psi.TolkVar
import org.ton.intellij.tolk.psi.TolkVarExpression
import org.ton.intellij.tolk.psi.impl.TolkStructFieldMixin
import org.ton.intellij.tolk.psi.impl.hasReceiver
import org.ton.intellij.tolk.psi.impl.isPrivate
import org.ton.intellij.tolk.psi.impl.isReadonly
import org.ton.intellij.tolk.psi.impl.receiverTy
import org.ton.intellij.tolk.psi.impl.returnTy
import org.ton.intellij.tolk.type.*
import org.ton.intellij.util.parentOfType
import java.util.function.Consumer
import kotlin.math.max

class TolkDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?) = when (element) {
        is TolkFunction           -> element.generateDoc()
        is TolkConstVar           -> element.generateDoc()
        is TolkGlobalVar          -> element.generateDoc()
        is TolkTypeDef            -> element.generateDoc()
        is TolkStruct             -> element.generateDoc()
        is TolkStructField        -> element.generateDoc()
        is TolkEnum               -> element.generateDoc()
        is TolkEnumMember         -> element.generateDoc()
        is TolkParameter          -> element.generateDoc()
        is TolkSelfParameter      -> element.generateDoc()
        is TolkSelfTypeExpression -> element.generateDoc()
        is TolkVar                -> element.generateDoc()
        is TolkTypeParameter      -> element.generateDoc()
        is TolkCatchParameter     -> element.generateDoc()
        is TolkAnnotation         -> element.generateDoc()
        else                      -> null
    }

    override fun getCustomDocumentationElement(editor: Editor, file: PsiFile, contextElement: PsiElement?, targetOffset: Int): PsiElement? {
        val parent = contextElement?.parent
        if (parent is TolkAnnotation) {
            return parent
        }

        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset)
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

    override fun getDocumentationElementForLink(psiManager: PsiManager?, link: String, context: PsiElement): PsiElement? {
        return resolveDocumentationReference(link, context)
    }
}

fun TolkTy.generateDoc(): String = buildString {
    when (this@generateDoc) {
        is TolkIntNTy         -> colorize(render(), asPrimitive)
        is TolkBitsNTy        -> colorize(render(), asPrimitive)
        is TolkBytesNTy       -> colorize(render(), asPrimitive)

        is TolkConstantBoolTy -> colorize(value.toString(), asKeyword)
        is TolkTyBool         -> colorize("bool", asPrimitive)

        TolkTy.Int            -> colorize("int", asPrimitive)
        TolkCellTy            -> colorize("cell", asPrimitive)
        TolkSliceTy           -> colorize("slice", asPrimitive)
        TolkStringTy          -> colorize("string", asPrimitive)
        TolkTy.Builder        -> colorize("builder", asPrimitive)
        TolkTy.Continuation   -> colorize("continuation", asPrimitive)
        TolkTy.Tuple          -> colorize("tuple", asPrimitive)
        TolkTy.Void           -> colorize("void", asPrimitive)
        TolkTy.Null           -> colorize("null", asKeyword)
        TolkTy.Coins          -> colorize("coins", asPrimitive)
        TolkTy.Address        -> colorize("address", asPrimitive)
        TolkTy.AnyAddress     -> colorize("any_address", asPrimitive)
        TolkTy.VarInt16       -> colorize("varint16", asPrimitive)
        TolkTy.VarUInt16      -> colorize("varuint16", asPrimitive)
        TolkTy.VarInt32       -> colorize("varint32", asPrimitive)
        TolkTy.VarUInt32      -> colorize("varuint32", asPrimitive)

        is TolkIntTyFamily    -> colorize(render(), asPrimitive)

        TolkTy.Unknown        -> colorize("unknown", asIdentifier)
        TolkTy.Never          -> colorize("never", asKeyword)

        is TolkTyStruct       -> {
            val name = psi.name ?: "Anonymous"
            if (name == "map" || name == "array") {
                colorize(name, asKeyword)
            } else {
                colorize(name, asStruct)
            }
            renderTypeParameters(typeArguments, this@buildString)
        }

        is TolkTyEnum         -> {
            colorize(psi.name ?: "Anonymous", asEnum)
        }

        is TolkTyAlias        -> {
            val typeName = psi.name ?: "Anonymous"
            val primitiveType = TolkPrimitiveTy.fromName(typeName)
            if (primitiveType != null) {
                colorize(typeName, asPrimitive)
            } else {
                colorize(typeName, asTypeAlias)
            }
            renderTypeParameters(typeArguments, this@buildString)
        }

        is TolkTyFunction     -> {
            colorize("fun ", asKeyword)
            colorize("(", asParen)
            parametersType.forEachIndexed { index, paramType ->
                if (index > 0) colorize(", ", asComma)
                append(paramType.generateDoc())
            }
            colorize(")", asParen)
            append(" ")
            colorize("->", asComma)
            append(" ")
            append(returnType.generateDoc())
        }

        is TolkTyTensor       -> {
            colorize("(", asParen)
            elements.forEachIndexed { index, element ->
                if (index > 0) colorize(", ", asComma)
                append(element.generateDoc())
            }
            colorize(")", asParen)
        }

        is TolkTyTypedTuple   -> {
            colorize("[", asParen)
            elements.forEachIndexed { index, element ->
                if (index > 0) colorize(", ", asComma)
                append(element.generateDoc())
            }
            colorize("]", asParen)
        }

        is TolkTyUnion        -> {
            val nullableType = orNull
            if (nullableType != null) {
                // nullable types like `int?`
                append(nullableType.generateDoc())
                colorize("?", asComma)
            } else {
                if (variants.size > 2) {
                    variants.forEach { variant ->
                        append("\n    ")
                        colorize("|", asComma)
                        append(" ")
                        append(variant.generateDoc())
                    }
                    return@buildString
                }
                variants.forEachIndexed { index, variant ->
                    if (index > 0) {
                        append(" ")
                        colorize("|", asComma)
                        append(" ")
                    }
                    append(variant.generateDoc())
                }
            }
        }

        is TolkTyParam        -> {
            colorize(name ?: "T", asTypeParameter)
        }

        else                  -> colorize(render(), asIdentifier)
    }
}

private fun renderTypeParameters(typeArguments: List<TolkTy>, builder: StringBuilder) {
    if (typeArguments.isNotEmpty()) {
        builder.colorize("<", asParen)
        typeArguments.forEachIndexed { index, typeArg ->
            if (index > 0) builder.colorize(", ", asComma)
            builder.append(typeArg.generateDoc())
        }
        builder.colorize(">", asParen)
    }
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
    val isPrimitiveType = TolkPrimitiveTy.fromName(name ?: "") != null || name == "map" || name == "array"

    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        line(annotations.annotations().generateDoc())

        part("type", asKeyword)
        colorize(name ?: "", if (isPrimitiveType) asPrimitive else asTypeAlias)

        val typeParams = typeParameterList
        if (typeParams != null) {
            append(typeParams.generateDoc())
        }

        append(" = ")

        if (isPrimitiveType || typeExpression?.text == "builtin") {
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

        val modifiers = mutableListOf<String>()
        if (isPrivate) modifiers.add("private")
        if (isReadonly) modifiers.add("readonly")
        
        if (modifiers.isNotEmpty()) {
            modifiers.forEach { modifier ->
                colorize(modifier, asKeyword)
                append(" ")
            }
        }

        colorize(name ?: "", asField)
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

fun TolkEnum.generateDoc(): String {
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        line(annotations.annotations().generateDoc())

        part("enum", asKeyword)

        colorize(name ?: "", asEnum)

        val serializationType = typeExpression?.type
        if (serializationType != null) {
            append(": ")
            append(serializationType.generateDoc())
        }

        generateEnumMembers(enumBody?.enumMemberList ?: emptyList())

        append(DocumentationMarkup.DEFINITION_END)
        generateCommentsPart(this@generateDoc)
    }
}

fun TolkEnumMember.generateDoc(): String {
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        val owner = parentOfType<TolkEnum>()
        if (owner != null && owner.name != null) {
            colorize("enum", asKeyword)
            append(" ")
            colorize(owner.name ?: "", asEnum)
            append("\n")
        }

        colorize(name ?: "", asEnumMember)

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

        val typeParams = typeParameterList
        if (typeParams != null) {
            append(typeParams.generateDoc())
        }

        append(parameters.generateDoc(parameterList?.selfParameter))
        append(": ")
        appendNotNull(returnType.generateDoc())

        append(DocumentationMarkup.DEFINITION_END)
        generateCommentsPart(this@generateDoc)
    }
}

fun Sequence<TolkAnnotation>.generateDoc(): String {
    return joinToString("\n") { attr ->
        attr.generateShortDoc()
    }
}

fun TolkAnnotation.generateDoc(): String {
    val name = identifier?.text ?: ""

    return buildString {
        append(DocumentationMarkup.DEFINITION_START)
        append(generateShortDoc())
        append(DocumentationMarkup.DEFINITION_END)

        val annotationInfo = TolkAnnotationInfo.getAnnotationInfo(name)
        if (annotationInfo != null) {
            append(DocumentationMarkup.CONTENT_START)
            append(annotationInfo.description)
            append(DocumentationMarkup.CONTENT_END)
        }
    }
}

fun TolkAnnotation.generateShortDoc(): String {
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
            if (isMutable) {
                colorize("mutate ", asKeyword)
            }
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
                
                val modifiers = mutableListOf<String>()
                if (field.isPrivate) modifiers.add("private")
                if (field.isReadonly) modifiers.add("readonly")
                
                if (modifiers.isNotEmpty()) {
                    modifiers.forEach { modifier ->
                        colorize(modifier, asKeyword)
                        append(" ")
                    }
                }
                
                colorize(field.name ?: "", asField)
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

private fun StringBuilder.generateEnumMembers(fields: List<TolkEnumMember>) {
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
                colorize(field.name ?: "", asEnumMember)

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

fun TolkParameter.generateDoc(): String {
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        if (isMutable) {
            part("mutate", asKeyword)
        }
        colorize(name ?: "", asParameter)
        append(": ")

        val type = type
        if (type != null) {
            append(type.generateDoc())
        }

        val defaultValue = parameterDefault?.expression
        if (defaultValue != null) {
            append(" = ")
            append(defaultValue.generateDoc())
        }

        append(DocumentationMarkup.DEFINITION_END)
    }
}

fun TolkSelfParameter.generateDoc(): String {
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        if (isMutable) {
            part("mutate", asKeyword)
        }
        colorize("self", asKeyword)

        val type = type
        if (type != null) {
            append(": ")
            append(type.generateDoc())
        }

        append(DocumentationMarkup.DEFINITION_END)
    }
}

fun TolkSelfTypeExpression.generateDoc(): String {
    val owner = parentOfType<TolkSelfParameter>() ?: return ""
    return owner.generateDoc()
}

fun TolkVar.generateDoc(): String {
    val varExpression = parentOfType<TolkVarExpression>()

    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        val kind = if (varExpression?.valKeyword != null) "val" else "var"

        part(kind, asKeyword)
        colorize(name ?: "", asIdentifier)

        val type = type
        if (type != null) {
            append(": ")
            append(type.generateDoc())
        }

        append(DocumentationMarkup.DEFINITION_END)
    }
}

fun TolkTypeParameter.generateDoc(): String {
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        part("type parameter", asKeyword)
        colorize(name ?: "", asTypeParameter)

        val defaultType = defaultTypeParameter?.typeExpression?.type
        if (defaultType != null) {
            append(" = ")
            append(defaultType.generateDoc())
        }

        append(DocumentationMarkup.DEFINITION_END)
        generateCommentsPart(null) // Type parameters don't have doc comments directly
    }
}

fun TolkCatchParameter.generateDoc(): String {
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        part("catch parameter", asKeyword)
        colorize(name ?: "", asParameter)

        val type = type
        if (type != null) {
            append(": ")
            append(type.generateDoc())
        }

        append(DocumentationMarkup.DEFINITION_END)
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
