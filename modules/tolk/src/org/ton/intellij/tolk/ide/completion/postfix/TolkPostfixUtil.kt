package org.ton.intellij.tolk.ide.completion.postfix

import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.TolkExpressionStatement
import org.ton.intellij.tolk.psi.TolkVarExpression

object TolkPostfixUtil {
    fun isExpression(context: PsiElement) = context.parentOfType<TolkExpressionStatement>() != null
    fun notInsideVarDeclaration(context: PsiElement) = context.parentOfType<TolkVarExpression>() == null

    fun startTemplate(string: String, project: Project, editor: Editor, vararg variables: Pair<String, Expression>) {
        val template = TemplateManager.getInstance(project)
            .createTemplate("templatePostfix", "tolk", string)
        template.isToReformat = true

        variables.forEach { (name, expression) ->
            if (expression is ConstantNode) {
                template.addVariable(name, expression, true)
                return@forEach
            }

            template.addVariable(name, expression, ConstantNode("_"), true)
        }

        TemplateManager.getInstance(project).startTemplate(editor, template)
    }
}