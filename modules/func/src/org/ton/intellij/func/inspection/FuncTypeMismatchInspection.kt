package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.rawReturnType
import org.ton.intellij.func.type.ty.FuncTyUnknown
import org.ton.intellij.util.parentOfType

class FuncTypeMismatchInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {

        override fun visitReturnStatement(returnStatement: FuncReturnStatement) {
            val outerFunction = returnStatement.parentOfType<FuncFunction>() ?: return
            val returnType = outerFunction.rawReturnType
            val expr = returnStatement.expression ?: return

            val inference = returnStatement.inference ?: return
            val exprType = inference.getExprTy(expr)

            if (exprType is FuncTyUnknown || returnType is FuncTyUnknown) return

            if (!returnType.canRhsBeAssigned(exprType)) {
                holder.registerProblem(
                    expr,
                    "Cannot return <code>${exprType}</code> from function with return type <code>${returnType}</code>",
                    ProblemHighlightType.GENERIC_ERROR,
                    ChangeReturnTypeFix(outerFunction, exprType.toString())
                )
            }
        }
    }

    private class ChangeReturnTypeFix(
        function: FuncFunction,
        private val newTypeText: String
    ) : LocalQuickFixAndIntentionActionOnPsiElement(function) {
        override fun getText(): String = "Change return type to '$newTypeText'"

        override fun getFamilyName(): String = "Change return type"

        override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
            val factory = FuncPsiFactory[project]
            val newTypeReference = factory.createTypeReference(newTypeText)
            (startElement as? FuncFunction)?.typeReference?.replace(newTypeReference)
        }
    }
}
