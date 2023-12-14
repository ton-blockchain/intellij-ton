package org.ton.intellij.func.inspection

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.refactoring.suggested.startOffset
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.collectArguments

class FuncReplaceGuardClauseWithFunctionCallInspection : FuncAbstractApplicabilityBasedInspection<FuncIfStatement>(
    FuncIfStatement::class.java
) {
    //    private val THROW_FUNCTION = "throw"
//
//    override fun buildFuncVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): FuncVisitor =
//        object : FuncVisitor() {
//            override fun visitIfStatement(o: FuncIfStatement) {
//                super.visitIfStatement(o)
//
//                val condition = o.expression ?: return
//                o.branchingStatement ?: return
//
//                val call = o.blockStatement.getCallExpression() ?: return
//
//                val arguments = call.callArgument.collectArguments()
//                val argument = arguments.firstOrNull() ?: return
//                if (arguments.size > 1) {
//                    return
//                }
//
//
//                val callReferenceText = call.referenceExpression.text
//                if (callReferenceText != THROW_FUNCTION) {
//                    return
//                }
//
//                val newExpression = FuncPsiFactory[o.project].createStatement("throw_if(${condition.text}, ${argument.text});")
////                o.replace(newExpression)
//
//                holder.registerProblem(
//                    o,
//                    "test?",
//                    object : LocalQuickFix {
//                        override fun getFamilyName(): String = "replace with throw_if function call"
//
//                        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
//                            descriptor.
//                        }
//
//                        override fun getName(): String = familyName
//                    }
//                )
//            }
//        }
//

    override val defaultFixText: String
        get() = "Replace with function call"

    override fun fixText(element: FuncIfStatement): String = when {
        element.ifKeyword != null -> "Replace with 'throw_if()' call"
        element.ifnotKeyword != null -> "Replace with 'throw_unless()' call"
        else -> super.fixText(element)
    }

    override fun inspectionText(element: FuncIfStatement): String = "Replace guard clause with function call"

    override fun isApplicable(element: FuncIfStatement): Boolean {
        element.condition ?: return false
        if (element.elseBranch != null || element.elseIfBranch != null) {
            return false // TODO: support else/elseIfBranch block's statements insert into current scope
        }
        val call = element.getCallExpression() ?: return false
        val calleeText = call.referenceExpression.text
        if (calleeText != THROW_FUNCTION) {
            return false
        }
        val arguments = call.callArgument.collectArguments()
        return arguments.size <= 1
    }

    override fun applyTo(element: FuncIfStatement, project: Project, editor: Editor?) {
        if (element.elseBranch != null || element.elseIfBranch != null) {
            return
        }

        val condition = element.condition ?: return
        val call = element.getCallExpression() ?: return
        val argument = call.callArgument.collectArguments().firstOrNull()?.text ?: return

        val psiFactory = FuncPsiFactory[project]

        val newExpression = if (element.ifnotKeyword != null) {
            psiFactory.createStatement(
                "throw_unless(${
                    argument.removeSurrounding(
                        "(",
                        ")"
                    )
                }, ${condition.text.removeSurrounding("(", ")")});"
            )
        } else {
            psiFactory.createStatement(
                "throw_if(${
                    argument.removeSurrounding(
                        "(",
                        ")"
                    )
                }, ${condition.text.removeSurrounding("(", ")")});"
            )
        }

        val replaced = element.replaceWith(newExpression, psiFactory)
        editor?.caretModel?.moveToOffset(replaced.startOffset)
    }

    private fun FuncIfStatement.replaceWith(newExpression: FuncStatement, psiFactory: FuncPsiFactory): FuncStatement {
        val parent = parent
        val elseBranch = elseBranch
        val elseIfBranchBranch = elseIfBranch

        if (elseBranch != null || elseIfBranchBranch != null) {
            TODO()
        } else {
            return replace(newExpression) as FuncStatement
        }
    }

    private fun FuncIfStatement.getCallExpression(): FuncCallExpression? {
        val expression = blockStatement?.let {
            it.statementList.firstOrNull() as? FuncExpressionStatement
        } ?: return null
        return expression.expression.let {
            it as? FuncCallExpression
        }
    }

    companion object {
        private const val THROW_FUNCTION = "throw"
    }
}
