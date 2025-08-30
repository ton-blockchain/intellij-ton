package org.ton.intellij.func.ide.fixes

import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.startOffset
import org.ton.intellij.func.psi.FuncConstVar
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncGlobalVar
import org.ton.intellij.util.parentOfType

abstract class FuncCreateTopLevelDeclarationQuickfix(identifier: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(identifier) {
    fun run(
        templateText: String,
        editor: Editor?,
        startElement: PsiElement,
        vararg variables: Pair<String, Expression>,
    ) {
        val project = startElement.project
        val template = TemplateManager.getInstance(project).createTemplate(
            "templateInsertHandler", "ton", """
                $templateText
                
                
            """.trimIndent()
        )
        template.isToReformat = true

        variables.forEach { (name, value) ->
            template.addVariable(name, value, true)
        }

        if (editor != null) {
            val currentTopLevel = startElement.parentOfType<FuncFunction>()
                ?: startElement.parentOfType<FuncConstVar>()
                ?: startElement.parentOfType<FuncGlobalVar>()
                ?: return
            editor.caretModel.moveToOffset(currentTopLevel.startOffset)
            TemplateManager.getInstance(project).startTemplate(editor, template)
        }
    }
}
