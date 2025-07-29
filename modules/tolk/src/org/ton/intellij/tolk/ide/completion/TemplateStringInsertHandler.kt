package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.TemplateManager

class TemplateStringInsertHandler(
    private val string: String,
    private val reformat: Boolean = true,
    private vararg val variables: Pair<String, Expression>,
) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val template = TemplateManager.getInstance(context.project).createTemplate("templateInsertHandler", "ton", string)
        template.isToReformat = reformat

        variables.forEach { (name, expression) ->
            template.addVariable(name, expression, true)
        }

        TemplateManager.getInstance(context.project).startTemplate(context.editor, template)
    }
}
