package org.ton.intellij.func.ide.fixes

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.func.psi.*

class RenameUnderscoreFix(
    val element: FuncReferenceExpression
) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    override fun getFamilyName(): String =
        CodeInsightBundle.message("rename.element.family")

    override fun getText(): String =
        CodeInsightBundle.message("rename.named.element.text", element.name, "_")

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val expression = FuncPsiFactory[project].createExpression("var (_) = 1") as FuncBinExpression
        val newElement = ((expression.left as FuncApplyExpression).right as FuncTensorExpression).expressionList.first()
        startElement.replace(newElement)
    }
}
