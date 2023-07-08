package org.ton.intellij.func.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import org.ton.intellij.func.FuncLanguage

object FuncElementFactory {

    fun createFileFromText(project: Project, text: String) =
        PsiFileFactory.getInstance(project).createFileFromText("dummy.fc", FuncLanguage, text) as FuncFile

    fun createIdentifierFromText(project: Project, text: String) {
//        val funcFile = createFileFromText(project, "const $text;")
//        val const = PsiTreeUtil.findChildOfType(funcFile, FuncConstVariable::class.java)
    }
}
