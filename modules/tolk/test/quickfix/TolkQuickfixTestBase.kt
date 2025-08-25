package org.ton.intellij.tolk.quickfix

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.openapi.command.WriteCommandAction
import org.intellij.lang.annotations.Language
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.replaceCaretMarker

abstract class TolkQuickfixTestBase : TolkTestBase() {
    override fun getTestDataPath(): String = "testResources/org.ton.intellij.tolk"

    protected open fun doQuickfixTest(
        @Language("Tolk") before: String,
        @Language("Tolk") after: String,
        quickfixText: String? = null,
        vararg inspections: InspectionProfileEntry,
    ) {
        myFixture.enableInspections(*inspections)
        myFixture.configureByText("test.tolk", replaceCaretMarker(before))

        val quickfix = myFixture.getAllQuickFixes().firstOrNull { it.familyName.contains(quickfixText ?: "") }
        assertNotNull("Quickfixes not found", quickfix)

        WriteCommandAction.runWriteCommandAction(project) {
            quickfix!!.invoke(project, myFixture.editor, myFixture.file)
        }

        // Wait for the template to finish if any
        val templateManager = TemplateManager.getInstance(project)
        if (templateManager.getActiveTemplate(myFixture.editor) != null) {
            templateManager.finishTemplate(myFixture.editor)
        }

        myFixture.checkResult(after)
    }
}
