package org.ton.intellij.tolk.ide.fixes

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.tolk.psi.*

// TODO: fix apply expressions
class RenameUnderscoreFix(
    val element: TolkReferenceExpression
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
//        val expression = TolkPsiFactory[project].createExpression("var (_) = 1") as TolkBinExpression
//        val newElement = ((expression.left as TolkApplyExpression).right as TolkTensorExpression).expressionList.first()
//        startElement.replace(newElement)
    }
}
