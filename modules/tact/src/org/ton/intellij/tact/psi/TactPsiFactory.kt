package org.ton.intellij.tact.psi

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import org.ton.intellij.tact.TactFileType
import org.ton.intellij.util.descendantOfTypeStrict

@Service(Service.Level.PROJECT)
class TactPsiFactory(
    private val project: Project
) {
    fun createFile(text: CharSequence): TactFile = createPsiFile(text) as TactFile

    fun createPsiFile(text: CharSequence): PsiFile = PsiFileFactory.getInstance(project)
        .createFileFromText(
            "DUMMY.tact",
            TactFileType,
            text,
        )

    fun createIdentifier(name: String): PsiElement =
        createFromText<TactStruct>("struct $name {}")?.identifier ?: error("Failed to create identifier")

    fun createExpression(text: String): PsiElement =
        createFromText<TactExpression>("fun dummy() { $text; }") ?: error("Failed to create expression")

    private inline fun <reified T : TactElement> createFromText(
        code: CharSequence
    ): T? = createFile(code).descendantOfTypeStrict()

    companion object {
        operator fun get(project: Project) =
            requireNotNull(project.getService(TactPsiFactory::class.java))
    }
}
