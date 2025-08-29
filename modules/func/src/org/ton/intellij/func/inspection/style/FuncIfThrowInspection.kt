package org.ton.intellij.func.inspection.style

import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.ton.intellij.func.FuncBundle
import org.ton.intellij.func.inspection.FuncInspectionBase
import org.ton.intellij.func.psi.*

enum class IfType { IF, IF_NOT }

class FuncIfThrowInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitIfStatement(ifStatement: FuncIfStatement) {
            val ifType = getIfStatementType(ifStatement)
            if (ifType == null || !isIfThrowPattern(ifStatement)) {
                return
            }

            val throwCall = extractThrowCall(ifStatement) ?: return
            val condition = ifStatement.condition ?: return

            val manager = SmartPointerManager.getInstance(throwCall.project)
            val (description, fixText) = when (ifType) {
                IfType.IF     -> FuncBundle.message("inspection.if.throw.description") to FuncBundle.message("inspection.if.throw.fix.text")
                IfType.IF_NOT -> FuncBundle.message("inspection.ifnot.throw.description") to FuncBundle.message("inspection.ifnot.throw.fix.text")
            }
            
            holder.registerProblem(
                ifStatement.firstChild ?: ifStatement,
                description,
                ProblemHighlightType.WEAK_WARNING,
                FuncIfThrowQuickFix(
                    ifStatement,
                    manager.createSmartPsiElementPointer(condition as PsiElement),
                    manager.createSmartPsiElementPointer(throwCall as PsiElement),
                    ifType,
                    fixText
                )
            )
        }
    }

    private fun getIfStatementType(ifStatement: FuncIfStatement): IfType? {
        val firstChild = ifStatement.firstChild
        return when (firstChild.text) {
            "if" -> IfType.IF
            "ifnot" -> IfType.IF_NOT
            else -> null
        }
    }

    private fun isIfThrowPattern(ifStatement: FuncIfStatement): Boolean {
        if (ifStatement.elseBranch != null) {
            return false
        }

        val blockStatement = ifStatement.blockStatement ?: return false
        val statements = blockStatement.statementList

        if (statements.size != 1) {
            return false
        }

        val statement = statements[0]
        return isThrowExpressionStatement(statement)
    }

    private fun isThrowExpressionStatement(statement: FuncStatement): Boolean {
        if (statement !is FuncExpressionStatement) {
            return false
        }

        return isThrowCall(statement.expression)
    }

    private fun isThrowCall(expression: FuncExpression): Boolean {
        if (expression !is FuncApplyExpression) {
            return false
        }

        val left = expression.left
        if (left !is FuncReferenceExpression) {
            return false
        }

        return left.identifier.text == "throw"
    }

    private fun extractThrowCall(ifStatement: FuncIfStatement): FuncApplyExpression? {
        val blockStatement = ifStatement.blockStatement ?: return null
        val statements = blockStatement.statementList

        if (statements.size != 1) {
            return null
        }

        val statement = statements[0]
        if (statement !is FuncExpressionStatement) {
            return null
        }

        val expression = statement.expression
        if (expression !is FuncApplyExpression) {
            return null
        }

        return expression
    }
}

class FuncIfThrowQuickFix(
    element: PsiElement,
    private val condition: SmartPsiElementPointer<PsiElement>,
    private val throwCall: SmartPsiElementPointer<PsiElement>,
    private val ifType: IfType,
    private val fixText: String
) : LocalQuickFixAndIntentionActionOnPsiElement(element), LocalQuickFix {

    override fun getFamilyName(): String = FuncBundle.message("inspection.if.throw.fix.family.name")
    override fun getText(): String = fixText

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) {
        val ifStatement = startElement as? FuncIfStatement ?: return

        val throwCallElement = throwCall.dereference() as? FuncApplyExpression ?: return
        val conditionElement = condition.dereference() as? FuncExpression ?: return
        
        val throwArg = throwCallElement.right?.unwrapTensor() ?: return
        val condition = conditionElement.unwrapTensor() ?: return

        val psiFactory = FuncPsiFactory[project]
        val functionName = when (ifType) {
            IfType.IF     -> "throw_if"
            IfType.IF_NOT -> "throw_unless"
        }

        val throwIfText = "$functionName(${throwArg.text}, ${condition.text});"
        val throwIfStatement = psiFactory.createStatement(throwIfText)

        ifStatement.replace(throwIfStatement)
    }

    fun FuncElement.unwrapTensor(): FuncElement? {
        if (this is FuncTensorExpression) {
            return this.expressionList.firstOrNull()
        }
        return this
    }
}
