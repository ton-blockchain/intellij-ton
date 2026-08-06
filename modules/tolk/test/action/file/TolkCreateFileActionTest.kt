package org.ton.intellij.tolk.action.file

import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.impl.CustomFileTemplate
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.ton.intellij.tolk.TolkTestBase

class TolkCreateFileActionTest : TolkTestBase() {
    fun `test creates contract without extension in template name`() {
        val directory = myFixture.addFileToProject("contracts/existing.tolk", "").containingDirectory
        val template = CustomFileTemplate("Test Tolk Contract", "tolk").apply {
            text = "contract \${NAME} {}"
        }
        val createFileFromTemplate = TolkCreateFileAction::class.java.getDeclaredMethod(
            "createFileFromTemplate",
            String::class.java,
            FileTemplate::class.java,
            PsiDirectory::class.java,
        ).apply {
            isAccessible = true
        }
        lateinit var createdFile: PsiFile

        WriteCommandAction.runWriteCommandAction(project) {
            createdFile = createFileFromTemplate.invoke(
                TolkCreateFileAction(),
                "Voter.tolk",
                template,
                directory,
            ) as PsiFile
        }

        assertEquals("Voter.tolk", createdFile.name)
        assertTrue(createdFile.text.startsWith("contract Voter {"))
        assertFalse(createdFile.text.contains("Voter.tolk"))
    }

    fun `test keeps template name without tolk extension`() {
        assertEquals("Voter", "Voter".withoutTolkExtension())
    }
}
