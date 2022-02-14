package com.github.andreypfau.intellijton.fift.psi

import com.github.andreypfau.intellijton.childOfType
import com.github.andreypfau.intellijton.fift.FiftFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory

val Project.fiftPsiFactory get() = FiftPsiFactory(this)

class FiftPsiFactory(val project: Project) {
    inline fun <reified T : FiftElement> createFromText(code: CharSequence): T? =
        PsiFileFactory.getInstance(project)
            .createFileFromText("DUMMY.fif", FiftFileType, code)
            .childOfType()
}