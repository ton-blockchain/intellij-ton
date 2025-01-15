package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TemplateSettings
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.util.ProcessingContext

class TolkKeywordCompletionProvider(
    val priority: Double,
    val keywords: List<String> = emptyList(),
    val insertHandler: InsertHandler<LookupElement>? = null,
    val insertSpace: Boolean = true
) : CompletionProvider<CompletionParameters>() {
    constructor(priority: Double, vararg keywords: String) : this(priority, keywords.toList(), null)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        parameters.position
        keywords.asReversed().forEachIndexed { index, s ->
            result.addElement(createKeywordLookupElement(s, priority + (index * 0.01)))
        }
    }

    private fun createKeywordLookupElement(keyword: String, priority: Double): LookupElement {
        val insertHandler = insertHandler ?: createTemplateBasedInsertHandler("tolk_lang_$keyword")
        return PrioritizedLookupElement.withPriority(
            LookupElementBuilder.create(keyword).withBoldness(true).withInsertHandler(insertHandler),
            priority
        )
    }

    private fun createTemplateBasedInsertHandler(templateId: String): InsertHandler<LookupElement> =
        InsertHandler { context, item ->
            val editor = context.editor
            val template = TemplateSettings.getInstance().getTemplateById(templateId)
            if (template != null) {
                editor.document.deleteString(context.startOffset, context.tailOffset)
                TemplateManager.getInstance(context.project).startTemplate(editor, template)
            } else {
                val currentOffset = editor.caretModel.offset
                val documentText = editor.document.immutableCharSequence
                if (insertSpace && (documentText.length <= currentOffset || documentText[currentOffset] != ' ')) {
                    EditorModificationUtil.insertStringAtCaret(editor, " ")
                }
            }
        }
}
