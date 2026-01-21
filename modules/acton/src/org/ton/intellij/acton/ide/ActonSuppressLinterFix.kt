package org.ton.intellij.acton.ide

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.ton.intellij.acton.ActonBundle

class ActonSuppressLinterFix(
    private val ruleName: String,
) : SuppressQuickFix {

    override fun getFamilyName(): String =
        if (ruleName == "all") ActonBundle.message("intention.family.name.suppress.all.acton.fix") else ActonBundle.message("intention.family.name.suppress.acton.fix")

    override fun getName(): String = if (ruleName == "all") ActonBundle.message(
        "intention.name.suppress.all.fix",
        ruleName
    ) else ActonBundle.message("intention.name.suppress.fix", ruleName)

    override fun isAvailable(project: Project, context: PsiElement): Boolean = context.isValid

    override fun isSuppressAll(): Boolean = false

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val file = element.containingFile ?: return
        val document = file.viewProvider.document ?: return
        val line = document.getLineNumber(element.textOffset)

        val lineStartOffset = document.getLineStartOffset(line)
        val text = document.charsSequence

        var i = lineStartOffset
        while (i < text.length && (text[i] == ' ' || text[i] == '\t')) {
            i++
        }
        val indentation = text.subSequence(lineStartOffset, i).toString()

        val suppressionComment = "${indentation}// acton-disable-next-line $ruleName\n"
        document.insertString(lineStartOffset, suppressionComment)
    }
}
