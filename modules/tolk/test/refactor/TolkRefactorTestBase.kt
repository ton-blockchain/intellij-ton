package org.ton.intellij.tolk.refactor

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.refactoring.RefactoringActionHandler
import org.intellij.lang.annotations.Language
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.replaceCaretMarker

abstract class TolkRefactorTestBase : TolkTestBase() {
    override fun getTestDataPath(): String = "testResources/org.ton.intellij.tolk"

    override fun setUp() {
        super.setUp()
        val file = myFixture.copyDirectoryToProject("tolk-stdlib", "tolk-stdlib")
        project.tolkSettings.stdlibPath = file.url
    }

    protected fun doRefactorTest(
        @Language("Tolk") before: String,
        @Language("Tolk") after: String,
        handler: RefactoringActionHandler
    ) {
        myFixture.configureByText("test.tolk", replaceCaretMarker(before))
        
        val dataContext = createDataContext()
        handler.invoke(project, myFixture.editor, myFixture.file, dataContext)
        
        myFixture.checkResult(after)
    }

    protected fun doRefactorTestWithSelection(
        @Language("Tolk") before: String,
        @Language("Tolk") after: String,
        handler: RefactoringActionHandler,
        selectionStart: String = "/*selection*/",
        selectionEnd: String = "/*selection-end*/"
    ) {
        val beforeWithoutMarkers = before.replace(selectionStart, "").replace(selectionEnd, "")
        val startOffset = before.indexOf(selectionStart)
        val endOffset = before.indexOf(selectionEnd) - selectionStart.length
        
        myFixture.configureByText("test.tolk", beforeWithoutMarkers)
        myFixture.editor.selectionModel.setSelection(startOffset, endOffset)
        
        val dataContext = createDataContext()
        handler.invoke(project, myFixture.editor, myFixture.file, dataContext)
        
        myFixture.checkResult(after)
    }

    private fun createDataContext(): DataContext {
        return SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.EDITOR, myFixture.editor)
            .add(CommonDataKeys.PSI_FILE, myFixture.file)
            .build()
    }
}
