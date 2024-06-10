package org.ton.intellij.func.inspection

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import org.ton.intellij.func.FuncLanguageLevel
import org.ton.intellij.func.ide.settings.funcSettings
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncVisitor

class FuncLanguageLevelInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitComment(comment: PsiComment) {
            super.visitComment(comment)
            val languageLevel = holder.project.funcSettings.state.languageLevel
            if (languageLevel >= FuncLanguageLevel.FUNC_0_5_0) return
            val text = comment.text
            if (!text.startsWith("//") && !text.startsWith("/*")) return
            holder.registerProblem(
                comment,
                "Modern-style comments supported only in language level 0.5.0+",
                ProblemHighlightType.GENERIC_ERROR,
                ChangeLanguageLevel(FuncLanguageLevel.FUNC_0_5_0)
            )
        }

        override fun visitFunction(o: FuncFunction) {
            super.visitFunction(o)
            val languageLevel = holder.project.funcSettings.state.languageLevel
            if (languageLevel >= FuncLanguageLevel.FUNC_0_5_0) return
            val pureKeyword = o.pureKeyword
            if (pureKeyword != null) {
                holder.registerProblem(
                    pureKeyword,
                    "`pure` keyword supported only in language level 0.5.0+",
                    ProblemHighlightType.GENERIC_ERROR,
                    ChangeLanguageLevel(FuncLanguageLevel.FUNC_0_5_0)
                )
            }
            val getKeyword = o.getKeyword
            if (getKeyword != null) {
                holder.registerProblem(
                    getKeyword,
                    "`get` keyword supported only in language level 0.5.0+",
                    ProblemHighlightType.GENERIC_ERROR,
                    ChangeLanguageLevel(FuncLanguageLevel.FUNC_0_5_0)
                )
            }
        }
    }

    class ChangeLanguageLevel(
        val languageLevel: FuncLanguageLevel
    ) : LocalQuickFix {
        override fun getFamilyName(): String = "Change project language level to: ${languageLevel.displayName}"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            project.funcSettings.state.languageLevel = languageLevel
        }
    }
}
