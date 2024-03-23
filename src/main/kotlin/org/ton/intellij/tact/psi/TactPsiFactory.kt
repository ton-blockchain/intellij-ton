package org.ton.intellij.tact.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.util.LocalTimeCounter
import org.ton.intellij.tact.TactFileType
import org.ton.intellij.util.descendantOfTypeStrict

class TactPsiFactory(
    private val project: Project,
    private val markGenerated: Boolean = true,
    private val eventSystemEnabled: Boolean = false
) {
    fun createFile(text: CharSequence): TactFile = createPsiFile(text) as TactFile

    fun createPsiFile(text: CharSequence): PsiFile = PsiFileFactory.getInstance(project)
        .createFileFromText(
            "DUMMY.tact",
            TactFileType,
            text,
            LocalTimeCounter.currentTime(),
            eventSystemEnabled,
            markGenerated
        )

    fun createIdentifier(name: String): PsiElement =
        createFromText<TactStruct>("struct $name {}")?.identifier ?: error("Failed to create identifier")

    private inline fun <reified T : TactElement> createFromText(
        code: CharSequence
    ): T? = createFile(code).descendantOfTypeStrict()
}
