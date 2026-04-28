package org.ton.intellij.tolk.inspection

import com.intellij.execution.TestStateStorage
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import org.ton.intellij.acton.runconfig.actonTestFailureState
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.ide.test.configuration.TolkTestLocator
import org.ton.intellij.tolk.psi.TolkFile
import java.util.Date

class TolkTestFailedLineInspectionAnchorTest : TolkTestBase() {
    fun `test failed element follows expect call after edits above`() {
        myFixture.configureByText(
            "counter.test.tolk",
            """
                get fun `test deploy starts at zero`() {
                    expect(contract.currentCounter()).toEqual(1);
                }
            """.trimIndent(),
        )

        val file = myFixture.file as TolkFile
        val function = file.functions.single()
        val state =
            TestStateStorage.Record(
                0,
                Date(),
                0,
                0,
                null,
                "expect(<actual>).toEqual(<expected>)",
                "${file.virtualFile.path}:2:5",
            )
        val locationUrl = TolkTestLocator.getTestUrl(function)
        val testFailureState = project.actonTestFailureState

        val initialElement =
            TolkTestFailedLineInspection.findFailedElement(
                function,
                state,
                locationUrl,
                testFailureState,
            )
        val initialOffset = initialElement.textOffset

        assertEquals("expect", initialElement.text)

        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.insertString(0, "fun helper() {}\n\n")
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val updatedFile = myFixture.file as TolkFile
        val updatedFunction = updatedFile.functions.single { it.name == function.name }
        val movedElement =
            TolkTestFailedLineInspection.findFailedElement(
                updatedFunction,
                state,
                locationUrl,
                testFailureState,
            )

        assertEquals("expect", movedElement.text)
        assertTrue(movedElement.textOffset > initialOffset)
    }
}
