package org.ton.intellij.tolk.ide.fixes

import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.startOffset
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkGlobalVar
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.util.parentOfType

abstract class TolkCreateTopLevelDeclarationQuickfix(identifier: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(identifier) {
    fun run(
        template: String,
        editor: Editor?,
        startElement: PsiElement,
        vararg variables: Pair<String, Expression>,
    ) {
        val project = startElement.project
        val template = TemplateManager.getInstance(project).createTemplate(
            "templateInsertHandler", "ton", """
                $template
                
                
            """.trimIndent()
        )
        template.isToReformat = true

        variables.forEach { (name, value) ->
            template.addVariable(name, value, true)
        }

        if (editor != null) {
            val currentTopLevel = startElement.parentOfType<TolkFunction>()
                ?: startElement.parentOfType<TolkStruct>()
                ?: startElement.parentOfType<TolkConstVar>()
                ?: startElement.parentOfType<TolkGlobalVar>()
                ?: return
            editor.caretModel.moveToOffset(currentTopLevel.startOffset)
            TemplateManager.getInstance(project).startTemplate(editor, template)
        }
    }
}
