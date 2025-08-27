package org.ton.intellij.tlb.doc

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.psi.PsiElement
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asBinaryTag
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asBuiltinType
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asIdentifier
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asKeyword
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asNumber
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asResultType
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asTypeParameter
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.colorize
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.part
import org.ton.intellij.tlb.ide.TlbSyntaxHighlighter
import org.ton.intellij.tlb.ide.completion.providers.TLB_BUILTIN_TYPES
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.TlbReference
import org.ton.intellij.tlb.psi.TlbResultType
import org.ton.intellij.tlb.psi.TlbTypeExpression
import org.ton.intellij.tlb.psi.TlbTypes
import kotlin.math.pow

class TlbDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        return when (element) {
            is TlbResultType     -> element.generateDoc()
            is TlbTypeExpression -> {
                val name = element.text
                if (TLB_BUILTIN_TYPES.containsKey(name)) {
                    return generateBuiltinTypeDoc(name)
                }

                val reference = element.reference as? TlbReference ?: return null
                val types = reference.multiResolve(false).toList().mapNotNull { it.element as? TlbResultType }
                return types.generateDoc()
            }

            else                 -> null
        }
    }
}

private fun generateBuiltinTypeDoc(name: String): String {
    val description = generateBuiltinTypeDescription(name)
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        part("builtin type", asKeyword)
        colorize(name, asBuiltinType)

        append(DocumentationMarkup.DEFINITION_END)

        append(DocumentationMarkup.CONTENT_START)
        append(description)
        append(DocumentationMarkup.CONTENT_END)
    }
}

private fun generateBuiltinTypeDescription(name: String): String? {
    val data = TLB_BUILTIN_TYPES[name]
    if (data != null && data != "") {
        return data
    }
    return generateTypeDoc(name)
}

fun TlbResultType.generateDoc(): String {
    return buildString {
        append(DocumentationMarkup.DEFINITION_START)

        val constructor = parent as? TlbConstructor
        if (constructor != null) {
            append(constructor.generateDoc(this@generateDoc))
        }

        append(DocumentationMarkup.DEFINITION_END)
    }
}

fun List<TlbResultType>.generateDoc(): String {
    return joinToString("\n\n") {
        buildString {
            append(DocumentationMarkup.DEFINITION_START)

            val resultType = this@generateDoc.firstOrNull()

            val constructor = it.parent as? TlbConstructor
            if (constructor != null) {
                append(constructor.generateDoc(resultType))
            }

            append(DocumentationMarkup.DEFINITION_END)
        }
    }
}

fun TlbConstructor.generateDoc(resultType: TlbResultType?): String {
    val text = text
    val highlighter = TlbSyntaxHighlighter
    val lexer = highlighter.highlightingLexer
    val builder = StringBuilder()
    lexer.start(text)

    val typeName = resultType?.name
    val paramTypes = resultType?.paramList?.typeExpressionList?.map { it.text }?.toSet() ?: emptySet()

    while (lexer.tokenType != null) {
        val type = lexer.tokenType
        val tokenText = lexer.tokenText

        builder.append(
            when {
                type == TlbTypes.NUMBER                  -> colorize(tokenText, asNumber)
                type == TlbTypes.BINARY_TAG              -> colorize(tokenText, asBinaryTag)
                TLB_BUILTIN_TYPES.containsKey(tokenText) -> colorize(tokenText, asBuiltinType)
                paramTypes.contains(tokenText)           -> colorize(tokenText, asTypeParameter)
                tokenText == typeName                    -> colorize(tokenText, asResultType)
                else                                     -> colorize(tokenText, asIdentifier)
            }
        )
        lexer.advance()
    }

    return builder.toString()
}

data class TypeDoc(
    val label: String,
    val range: String,
    val size: String,
    val description: String? = null,
)

fun generateTypeDoc(word: String): String? {
    val typeInfo = generateArbitraryIntDoc(word) ?: return null

    return buildString {
        append(typeInfo.label)
        append("<ul>")
        append("<li><b>Range</b>: ${typeInfo.range}</li>")
        append("<li><b>Size</b>: ${typeInfo.size}</li>")
        if (typeInfo.description != null) {
            append("<li><b>Description</b>: ${typeInfo.description}</li>")
        }
        append("</ul>")
    }
}

fun generateArbitraryIntDoc(type: String): TypeDoc? {
    val match = Regex("^(u?int|bits)(\\d+)$").find(type) ?: return null
    val (prefix, bitsStr) = match.destructured
    val bitWidth = bitsStr.toIntOrNull() ?: return null

    when (prefix) {
        "uint" -> {
            if (bitWidth !in 1..256) return null
            return TypeDoc(
                label = "$bitWidth-bit unsigned integer",
                range = "0 to ${(2.0.pow(bitWidth) - 1).toLong()}",
                size = "$bitWidth bits",
                description = "Arbitrary bit-width unsigned integer type"
            )
        }

        "int"  -> {
            if (bitWidth !in 1..257) return null
            val min = -(2.0.pow(bitWidth - 1)).toLong()
            val max = (2.0.pow(bitWidth - 1) - 1).toLong()
            return TypeDoc(
                label = "$bitWidth-bit signed integer",
                range = "$min to $max",
                size = "$bitWidth bits",
                description = "Arbitrary bit-width signed integer type"
            )
        }

        "bits" -> {
            if (bitWidth !in 1..257) return null
            return TypeDoc(
                label = "$bitWidth-bit data",
                range = "0 to $bitWidth bits",
                size = "$bitWidth bits",
                description = "Arbitrary bit-width data"
            )
        }

        else   -> return null
    }
}
