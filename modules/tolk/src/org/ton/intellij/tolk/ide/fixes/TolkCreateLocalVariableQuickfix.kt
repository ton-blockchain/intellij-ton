package org.ton.intellij.tolk.ide.fixes

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.startOffset
import org.ton.intellij.tolk.psi.TolkStatement
import org.ton.intellij.util.parentOfType

class TolkCreateLocalVariableQuickfix(identifier: PsiElement) :
    LocalQuickFixAndIntentionActionOnPsiElement(identifier), HighPriorityAction {

    val actualName = identifier.text ?: ""

    override fun getFamilyName(): String = "Create local variable '$actualName'"
    override fun getText(): String = "Create local variable '$actualName'"

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) {
        val template = TemplateManager.getInstance(project).createTemplate(
            "templateInsertHandler", "ton", """
                val $actualName = ${"$"}value$${"$"}END$;
                
            """.trimIndent()
        )
        template.isToReformat = true

        template.addVariable("value", ConstantNode("0"), true)

        if (editor != null) {
            val currentStatement = startElement.parentOfType<TolkStatement>() ?: return
            editor.caretModel.moveToOffset(currentStatement.startOffset)
            TemplateManager.getInstance(project).startTemplate(editor, template)
        }
    }
}
