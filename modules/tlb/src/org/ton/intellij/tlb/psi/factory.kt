package org.ton.intellij.tlb.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.tlb.TlbFileType

val Project.tlbPsiFactory get() = TlbPsiFactory(this)

class TlbPsiFactory(val project: Project) {
    inline fun <reified T : PsiElement> createFromText(code: CharSequence): T? {
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText("DUMMY.tlb", TlbFileType, code)

        return PsiTreeUtil.findChildrenOfAnyType(file, T::class.java).firstOrNull()
    }

    fun createIdentifier(text: String) = createFromText<TlbConstructor>("$text#_ = $text;")!!.identifier
}
