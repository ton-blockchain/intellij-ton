package org.ton.intellij.tolk.ide.fixes

import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.type.TolkTyUnknown
import org.ton.intellij.tolk.type.render
import org.ton.intellij.util.parentOfType

class TolkCreateFunctionQuickfix(identifier: PsiElement) : TolkCreateTopLevelDeclarationQuickfix(identifier) {
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
        val call = startElement.parentOfType<TolkCallExpression>() ?: return
        val arguments = call.argumentList.argumentList
        val parameters = arguments.mapIndexed { index, arg ->
            val expr = arg.expression
            val argTy = expr.type ?: TolkTyUnknown
            val name = if (expr is TolkReferenceExpression) expr.text else "param${index}"

            return@mapIndexed name to argTy
        }

        val signature = parameters.joinToString(", ", "(", ")") { (name, argTy) -> "$$name$: ${argTy.render()}" }

        val template = """
            fun $actualName$signature {
                ${"$"}END$
            }
        """.trimIndent()
        run(template, editor, startElement, *parameters.map { (name) -> name to ConstantNode(name) }.toTypedArray())
    }
}
