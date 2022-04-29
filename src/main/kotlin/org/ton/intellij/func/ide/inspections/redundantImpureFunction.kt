package org.ton.intellij.func.ide.inspections

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.func.psi.*
import org.ton.intellij.processElements

class RedundantImpureFunctionAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is FuncFunction) return
        val impureSpecifier = element.impureSpecifier ?: return
        val blockStatement = element.blockStatement ?: return

        val resolvedFunctionReferences = ArrayList<FuncFunction>()
        blockStatement.processElements { statement ->
            val resolvedFuncFunction = resolveFuncFunctionReference(statement)
            if (resolvedFuncFunction != null) {
                resolvedFunctionReferences.add(resolvedFuncFunction)
            }
            true
        }

        val isRedundantSpecifier = resolvedFunctionReferences.all { funcFunction ->
            funcFunction.impureSpecifier == null
        }
        if (isRedundantSpecifier) {
            holder.newAnnotation(HighlightSeverity.INFORMATION,  "Redundant 'impure' specifier")
                    .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)
                    .range(impureSpecifier)
                    .withFix(RedundantImpureFunctionQuickFix(element))
                    .create()
        }
    }
}

class RedundantImpureFunctionQuickFix(val funcFunction: FuncFunction) : BaseIntentionAction() {

    override fun getText(): String = "Remove redundant 'impure' specifier"

    override fun getFamilyName(): String = "Impure functions"

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = true

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val funcImpureSpecifier = funcFunction.impureSpecifier ?: return

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                funcFunction.node.removeChild(funcImpureSpecifier.node)
            }
        }
    }
}

private fun resolveFuncFunctionReference(element: PsiElement): FuncFunction? {
    if (element !is FuncFunctionCall && element !is FuncMethodCall && element !is FuncModifyingMethodCall) return null
    return element.reference?.resolve() as? FuncFunction ?: return null
}