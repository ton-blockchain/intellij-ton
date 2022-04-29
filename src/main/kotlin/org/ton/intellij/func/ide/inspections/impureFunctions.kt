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
import com.intellij.psi.impl.source.tree.AstBufferUtil
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.FuncImpureSpecifierImpl
import org.ton.intellij.parentOfType

class ImpureFunctionAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is FuncFunctionCall && element !is FuncMethodCall) return
        val resolvedFunction = element.reference?.resolve() as? FuncFunction ?: return
        if (resolvedFunction.impureSpecifier == null) return
        val parentFuncFunction = element.parentOfType<FuncFunction>()
        if (parentFuncFunction == null || parentFuncFunction.impureSpecifier != null) return

        val elementName = (element as? FuncFunctionCall)?.nameIdentifier
                ?: (element as? FuncMethodCall)?.nameIdentifier
                ?: return
        val message = "Impure function '${resolvedFunction.name}' should be called only from another impure function"

        holder.newAnnotation(HighlightSeverity.WARNING, message)
                .range(elementName)
                .highlightType(ProblemHighlightType.WARNING)
                .withFix(ImpureFunctionQuickFix(parentFuncFunction))
                .create()
    }
}

class ImpureFunctionQuickFix(val funcFunction: FuncFunction) : BaseIntentionAction() {

    override fun getText(): String = "Add 'impure' specifier"

    override fun getFamilyName(): String = "Impure functions"

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = true

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val whitespaceNode = project.funcPsiFactory.createFile(" ").firstChild.node
        val impureSpecifierNode = createImpureSpecifierNode(project)
        val anchorNode = funcFunction.inlineSpecifier?.node ?:
        funcFunction.methodIdSpecifier?.node ?:
        funcFunction.blockStatement?.node ?:
        funcFunction.asmFunctionBody?.node

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                funcFunction.node.addChild(impureSpecifierNode, anchorNode)
                // TODO: replace manual whitespace addition with formatter:
                //  https://plugins.jetbrains.com/docs/intellij/modifying-psi.html#whitespaces-and-imports
                funcFunction.node.addChild(whitespaceNode, anchorNode)
            }
        }
    }

    private fun createImpureSpecifierNode(project: Project) =
            requireNotNull(project.funcPsiFactory.createFromText<FuncFunction>("() dummy() impure {}")).impureSpecifier!!.node
}