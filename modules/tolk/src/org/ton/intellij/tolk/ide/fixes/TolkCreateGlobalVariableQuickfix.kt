package org.ton.intellij.tolk.ide.fixes

import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class TolkCreateGlobalVariableQuickfix(identifier: PsiElement) : TolkCreateTopLevelDeclarationQuickfix(identifier) {
    val actualName = identifier.text ?: ""

    override fun getFamilyName(): String = "Create global variable '$actualName'"
    override fun getText(): String = "Create global variable '$actualName'"

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) = run("global $actualName: \$type$\$END$;", editor, startElement, "type" to ConstantNode("int"))
}
