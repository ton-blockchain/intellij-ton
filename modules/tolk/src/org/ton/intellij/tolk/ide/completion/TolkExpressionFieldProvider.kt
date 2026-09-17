package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.icons.AllIcons
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.startOffset
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.ide.TolkStructFields
import org.ton.intellij.tolk.psi.TolkStructExpression
import org.ton.intellij.tolk.psi.TolkStructExpressionField
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.tolk.psi.impl.canUse
import org.ton.intellij.tolk.psi.impl.hasPrivateFields
import org.ton.intellij.tolk.psi.impl.structFields
import org.ton.intellij.tolk.type.TolkTyStruct

object TolkExpressionFieldProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> = psiElement()
        .withParent(TolkStructExpressionField::class.java)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val element = parameters.position
        val parent = element.parent as? TolkStructExpressionField ?: return
        if (parent.identifier != element) return

        val structExpr = parent.parentOfType<TolkStructExpression>() ?: return
        val initedFields = structExpr.structExpressionBody.structExpressionFieldList
        val isEmptyStructExpression =
            initedFields.isEmpty() || initedFields.size == 1 && initedFields[0].text == "IntellijIdeaRulezzz"

        val originalStructExpr = parameters.originalPosition?.parentOfType<TolkStructExpression>() ?: return
        val document = parameters.editor.document
        val startLine = document.getLineNumber(originalStructExpr.structExpressionBody.startOffset)
        val endLine = document.getLineNumber(originalStructExpr.structExpressionBody.endOffset)
        val singleLine = startLine == endLine
        val comma = if (!singleLine) "," else ""
        val caretOffset = parameters.editor.caretModel.offset
        val existingFieldNameSuffixLength = originalStructExpr.structExpressionBody.structExpressionFieldList
            .asSequence()
            .filter { it.colon != null }
            .map { it.identifier }
            .firstOrNull { caretOffset in it.startOffset..it.endOffset }
            ?.let { it.endOffset - caretOffset }

        val structTy = structExpr.type?.unwrapTypeAlias() as? TolkTyStruct ?: return
        val initedFieldNames = initedFields.mapNotNull {
            if (it == parent) return@mapNotNull null
            it.referenceName
        }

        val ctx = TolkCompletionContext(parent)
        val structDecl = structTy.psi

        structDecl.structFields.asReversed().forEachIndexed { index, field ->
            if (field.name in initedFieldNames) return@forEachIndexed

            if (!field.canUse(structTy, element)) return@forEachIndexed

            val insertHandler = existingFieldNameSuffixLength?.let {
                ReplaceFieldNameInsertHandler(it)
            } ?: TemplateStringInsertHandler(
                ": \$value$$comma",
                true,
                "value" to ConstantNode(TolkStructFields.typeDefaultValue(field.type, parameters.originalFile)),
            )

            result.addElement(
                PrioritizedLookupElement.withPriority(
                    field.toLookupElementBuilder(ctx)
                        .withInsertHandler(insertHandler)
                        .toTolkLookupElement(
                            TolkLookupElementData(elementKind = TolkLookupElementData.ElementKind.FIELD),
                        ),
                    index.toDouble() * 0.001 + 1.0,
                ),
            )
        }

        val allFields = structDecl.structFields
        val requiredFields = allFields.filter { isFieldRequired(it) }

        if (isEmptyStructExpression && !structDecl.hasPrivateFields) {
            result.addElement(
                LookupElementBuilder.create("0")
                    .withPresentableText("Fill all fields…")
                    .withIcon(AllIcons.Actions.RealIntentionBulb)
                    .withInsertHandler(FillFieldsInsertHandler(allFields))
                    .withPriority(TolkCompletionPriorities.KEYWORD),
            )

            // no need to show this variant if it works like `Fill all fields` and there are some required fields to fill
            if (allFields.size != requiredFields.size && requiredFields.isNotEmpty()) {
                result.addElement(
                    LookupElementBuilder.create("1")
                        .withPresentableText("Fill required fields…")
                        .withIcon(AllIcons.Actions.RealIntentionBulb)
                        .withInsertHandler(FillFieldsInsertHandler(requiredFields))
                        .withPriority(TolkCompletionPriorities.KEYWORD),
                )
            }
        }
    }

    private class ReplaceFieldNameInsertHandler(private val oldNameSuffixLength: Int) : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            if (context.completionChar == Lookup.REPLACE_SELECT_CHAR) return

            val oldNameStart = context.tailOffset
            context.document.deleteString(oldNameStart, oldNameStart + oldNameSuffixLength)
        }
    }

    class FillFieldsInsertHandler(private val fields: List<TolkStructField>) : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val start = context.startOffset
            context.document.deleteString(start, start + 1)
            TolkStructFields.startTemplate(
                context.project,
                context.editor,
                TolkStructFields.fieldsTemplateText(fields),
                TolkStructFields.fieldsVariables(fields, context.file),
            )
        }
    }

    private fun isFieldRequired(field: TolkStructField): Boolean = field.expression == null
}
