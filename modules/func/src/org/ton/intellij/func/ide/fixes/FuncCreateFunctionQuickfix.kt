package org.ton.intellij.func.ide.fixes

import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.type.ty.FuncTyUnknown
import org.ton.intellij.util.parentOfType

class FuncCreateFunctionQuickfix(identifier: PsiElement) : FuncCreateTopLevelDeclarationQuickfix(identifier) {
    val actualName = identifier.text ?: ""

    override fun getFamilyName(): String = "Create function '$actualName'"
    override fun getText(): String = "Create function '$actualName'"

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) {
        val call = findFunctionCall(startElement) ?: return

        val (parameters, returnType) = extractParametersFromCall(call)

        val signature = if (parameters.isNotEmpty()) {
            parameters.joinToString(", ", "(", ")") { (name, type) -> "$type $$name$" }
        } else {
            "()"
        }

        val template = """
            $returnType $actualName$signature {
                ${"$"}END$
            }
        """.trimIndent()

        val variables = parameters.map { (name, _) -> name to ConstantNode(name) }.toTypedArray()
        run(template, editor, startElement, *variables)
    }

    private fun findFunctionCall(element: PsiElement): FuncApplyExpression? {
        return element.parentOfType<FuncApplyExpression>()?.takeIf {
            it.left is FuncReferenceExpression && (it.left as FuncReferenceExpression).text == actualName
        } ?: element.parentOfType<FuncSpecialApplyExpression>()?.let { specialApply ->
            (specialApply.left as? FuncApplyExpression)?.takeIf {
                it.left is FuncReferenceExpression && (it.left as FuncReferenceExpression).text == actualName
            }
        }
    }

    private fun extractParametersFromCall(call: FuncApplyExpression): Pair<List<Pair<String, String>>, String> {
        val arguments = when (val right = call.right) {
            is FuncTensorExpression    -> right.expressionList
            is FuncParenExpression     -> listOfNotNull(right.expression)
            is FuncReferenceExpression -> listOf(right)
            else                       -> emptyList()
        }

        val parameters = arguments.mapIndexed { index, arg ->
            val inference = arg.inference
            val argType = inference?.getExprTy(arg)
            val typeString = when {
                argType != null && argType !is FuncTyUnknown -> argType.toString()
                else                                         -> "int"
            }

            val name = if (arg is FuncReferenceExpression) {
                arg.text ?: "param$index"
            } else {
                "param$index"
            }

            name to typeString
        }

        return parameters to "()"
    }
}
