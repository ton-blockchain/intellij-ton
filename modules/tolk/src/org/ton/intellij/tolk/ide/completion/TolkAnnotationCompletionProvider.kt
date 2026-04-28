package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkAnnotation
import org.ton.intellij.tolk.psi.TolkAnnotationHolder
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.tolk.psi.impl.isEntryPoint
import org.ton.intellij.tolk.psi.impl.isGetMethod

typealias Applicability = (PsiElement) -> Boolean

object TolkAnnotationCompletionProvider : TolkCompletionProvider(), DumbAware {
    private val getMethodPattern = Regex("""(?:^|\s)get\s+fun\b""")

    override val elementPattern: ElementPattern<out PsiElement>
        get() = psiElement().inside(TolkAnnotation::class.java)

    val forAny: Applicability = { true }
    val forFunctions: Applicability = { it is TolkFunction }
    val forGetMethods: Applicability = { it is TolkFunction && it.isGetMethod }
    val forStructs: Applicability = { it is TolkStruct }
    val forStructFields: Applicability = { it is TolkStructField }
    val forEntryPoints: Applicability = { it is TolkFunction && it.isEntryPoint }

    private data class AnnotationLookupElement(
        val element: LookupElementBuilder,
        val applicability: Applicability,
        val fullName: String = element.lookupString,
    )

    private val rootLookupElements = listOf(
        annotationLookup("pure", forFunctions),
        annotationLookup("noinline", forFunctions),
        annotationLookup("inline", forFunctions),
        annotationLookup("inline_ref", forFunctions),
        annotationLookup("test", forGetMethods),
        annotationLookup("method_id", forFunctions, RequiredParInsertHandler),
        annotationLookup("abi.minimalMsgValue", forStructs, RequiredParInsertHandler, tailText = "(...)"),
        annotationLookup("abi.preferredSendMode", forStructs, RequiredParInsertHandler, tailText = "(...)"),
        annotationLookup("abi.clientType", forStructFields, RequiredParInsertHandler, tailText = "(...)"),
        annotationLookup(
            "deprecated",
            forAny,
            StringArgumentInsertHandler(""),
            tailText = "(\"reason\")",
        ),
        annotationLookup("custom", forAny, RequiredParInsertHandler),
        annotationLookup(
            "on_bounced_policy",
            forEntryPoints,
            StringArgumentInsertHandler("manual"),
            tailText = "(\"manual\")",
        ),
        annotationLookup(
            "overflow1023_policy",
            forStructs,
            tailText = "(\"suppress\")",
            insertHandler = InsertHandler { ctx, item ->
                RequiredParInsertHandler.handleInsert(ctx, item)
                val offset = ctx.editor.caretModel.offset
                val chars = ctx.document.charsSequence

                if (offset < chars.length && chars[offset] == ')' && offset > 0 && chars[offset - 1] == '(') {
                    ctx.document.insertString(offset, "\"suppress\"")
                    ctx.editor.caretModel.moveToOffset(offset + "\"suppress\")".length)
                }
            },
        ),
    )

    private val testLookupElements = listOf(
        annotationLookup("skip", forGetMethods, fullName = "test.skip"),
        annotationLookup("todo", forGetMethods, fullName = "test.todo"),
        annotationLookup(
            "todo",
            forGetMethods,
            StringArgumentInsertHandler(""),
            tailText = "(...)",
            fullName = "test.todo",
        ),
        annotationLookup(
            "fail_with",
            forGetMethods,
            RequiredParInsertHandler,
            tailText = "(...)",
            fullName = "test.fail_with",
        ),
        annotationLookup(
            "gas_limit",
            forGetMethods,
            RequiredParInsertHandler,
            tailText = "(...)",
            fullName = "test.gas_limit",
        ),
        annotationLookup(
            "fuzz",
            forGetMethods,
            fullName = "test.fuzz",
        ),
        annotationLookup(
            "fuzz",
            forGetMethods,
            RequiredParInsertHandler,
            tailText = "(...)",
            fullName = "test.fuzz",
        ),
    )

    private val abiLookupElements = listOf(
        annotationLookup(
            "minimalMsgValue",
            forStructs,
            RequiredParInsertHandler,
            tailText = "(...)",
            fullName = "abi.minimalMsgValue",
        ),
        annotationLookup(
            "preferredSendMode",
            forStructs,
            RequiredParInsertHandler,
            tailText = "(...)",
            fullName = "abi.preferredSendMode",
        ),
        annotationLookup(
            "clientType",
            forStructFields,
            RequiredParInsertHandler,
            tailText = "(...)",
            fullName = "abi.clientType",
        ),
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val pathPrefix = annotationPathPrefix(parameters) ?: return
        val owner = parameters.originalPosition?.parentOfType<TolkAnnotationHolder>()
            ?: parameters.position.parentOfType<TolkAnnotationHolder>()
        val lookupElements = when (pathPrefix.substringBefore('.')) {
            "test" -> if ('.' in pathPrefix) testLookupElements else rootLookupElements
            "abi" -> if ('.' in pathPrefix) abiLookupElements else rootLookupElements
            else -> if ('.' in pathPrefix) return else rootLookupElements
        }
        val segmentPrefix = pathPrefix.substringAfterLast('.', pathPrefix)
        val resultSet = if ('.' in pathPrefix) {
            result.withPrefixMatcher(PlainPrefixMatcher(segmentPrefix))
        } else {
            result
        }

        if (owner != null) {
            val currentAnnotations = owner.annotations.names().toSet()
            resultSet.addAllElements(
                lookupElements.filter { lookup ->
                    !currentAnnotations.contains(lookup.fullName) && lookup.isApplicable(owner)
                }.map { it.element },
            )
            return
        }

        resultSet.addAllElements(lookupElements.map { it.element })
    }

    private fun annotationLookup(
        name: String,
        applicability: Applicability,
        insertHandler: InsertHandler<LookupElement>? = null,
        tailText: String? = null,
        fullName: String = name,
    ): AnnotationLookupElement {
        var element = LookupElementBuilder.create(name).withIcon(TolkIcons.ANNOTATION)
        if (tailText != null) {
            element = element.withTailText(tailText)
        }
        if (insertHandler != null) {
            element = element.withInsertHandler(insertHandler)
        }
        return AnnotationLookupElement(element, applicability, fullName)
    }

    private fun AnnotationLookupElement.isApplicable(owner: PsiElement): Boolean {
        if (applicability(owner)) return true
        if (fullName.substringBefore('.') != "test") return false
        if (owner !is TolkFunction) return false
        return owner.annotations.hasAnnotation("method_id") || getMethodPattern.containsMatchIn(owner.text)
    }

    private fun annotationPathPrefix(parameters: CompletionParameters): String? {
        val fileText = parameters.originalFile.text
        val offset = parameters.editor.caretModel.offset.coerceAtMost(fileText.length)

        var start = offset
        while (start > 0) {
            val char = fileText[start - 1]
            if (char.isLetterOrDigit() || char == '_' || char == '.') {
                start--
                continue
            }
            break
        }
        if (start == 0 || fileText[start - 1] != '@') return null
        return fileText.substring(start, offset)
    }

    private object RequiredParInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            insertParentheses(context)
        }
    }

    private class StringArgumentInsertHandler(private val value: String) : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            TemplateStringInsertHandler(
                "(\"\$value$\")",
                true,
                "value" to ConstantNode(value),
            ).handleInsert(context, item)
        }
    }

    private fun insertParentheses(context: InsertionContext, contents: String = "", caretOffset: Int = 1) {
        val offset = context.editor.caretModel.offset
        val chars = context.document.charsSequence
        val absoluteOpeningBracketOffset = chars.indexOfSkippingSpace('(', offset)

        if (absoluteOpeningBracketOffset == null) {
            context.document.insertString(offset, "($contents)")
            context.editor.caretModel.moveToOffset(offset + caretOffset)
            context.commitDocument()
        }
    }
}
