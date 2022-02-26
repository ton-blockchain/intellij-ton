package com.github.andreypfau.intellijton.tlb.psi

import com.github.andreypfau.intellijton.childOfType
import com.github.andreypfau.intellijton.tlb.TlbFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory

val Project.tlbPsiFactory get() = TlbPsiFactory(this)

class TlbPsiFactory(val project: Project) {
    inline fun <reified T : TlbElement> createFromText(code: CharSequence): T? =
        PsiFileFactory.getInstance(project)
            .createFileFromText("DUMMY.tlb", TlbFileType, code)
            .childOfType()
}