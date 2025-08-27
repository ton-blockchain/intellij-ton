package org.ton.intellij.tlb.doc

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.psi.PsiElement
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asBinaryTag
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asBuiltinType
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asIdentifier
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asNumber
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asResultType
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.asTypeParameter
import org.ton.intellij.tlb.doc.TlbDocumentationUtils.colorize
import org.ton.intellij.tlb.ide.TlbSyntaxHighlighter
import org.ton.intellij.tlb.ide.completion.providers.BUILTIN_TYPES
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.TlbReference
import org.ton.intellij.tlb.psi.TlbResultType
import org.ton.intellij.tlb.psi.TlbTypeExpression
import org.ton.intellij.tlb.psi.TlbTypes

class TlbDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        return when (element) {
            is TlbResultType     -> element.generateDoc()
            is TlbTypeExpression -> {
                val reference = element.reference as? TlbReference ?: return null
                val types = reference.multiResolve(false).toList().mapNotNull { it.element as? TlbResultType }
                return types.generateDoc()
            }

            else                 -> null
        }
    }
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
                type == TlbTypes.NUMBER              -> colorize(tokenText, asNumber)
                type == TlbTypes.BINARY_TAG          -> colorize(tokenText, asBinaryTag)
                BUILTIN_TYPES.containsKey(tokenText) -> colorize(tokenText, asBuiltinType)
                paramTypes.contains(tokenText)       -> colorize(tokenText, asTypeParameter)
                tokenText == typeName                -> colorize(tokenText, asResultType)
                else                                 -> colorize(tokenText, asIdentifier)
            }
        )
        lexer.advance()
    }

    return builder.toString()
}
