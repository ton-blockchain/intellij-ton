package org.ton.intellij.fift.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import org.ton.intellij.childOfType
import org.ton.intellij.fift.FiftFileType

val Project.fiftPsiFactory get() = FiftPsiFactory(this)

class FiftPsiFactory(val project: Project) {
    inline fun <reified T : FiftElement> createFromText(code: CharSequence): T? =
        PsiFileFactory.getInstance(project)
            .createFileFromText("DUMMY.fif", FiftFileType, code)
            .childOfType()
}