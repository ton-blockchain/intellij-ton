package org.ton.intellij.func.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import org.ton.intellij.func.FuncLanguage

object FuncElementFactory {

    fun createFileFromText(project: Project, text: String) {
        PsiFileFactory.getInstance(project).createFileFromText("dummy.fc", FuncLanguage, text)
    }

    fun createIdentifierFromText(text: String) {

    }
}
