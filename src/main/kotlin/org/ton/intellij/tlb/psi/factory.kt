package org.ton.intellij.tlb.psi

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.ton.intellij.func.psi.FuncPsiFactory
import org.ton.intellij.tlb.TlbFileType
import org.ton.intellij.util.childOfType

val Project.tlbPsiFactory get() = TlbPsiFactory(this)

@Service(Service.Level.PROJECT)
class TlbPsiFactory(val project: Project) {
    inline fun <reified T : PsiElement> createFromText(code: CharSequence): T? =
        PsiFileFactory.getInstance(project)
            .createFileFromText("DUMMY.tlb", TlbFileType, code)
            .childOfType()

    fun createIdentifier(text: String): PsiElement {
        val constructor = requireNotNull((createFromText<TlbTypeDef>("$text#_ = $text;")))
        return requireNotNull(constructor.identifier)
    }

    companion object {
        operator fun get(project: Project) =
            requireNotNull(project.getService(TlbPsiFactory::class.java))
    }
}
