package org.ton.intellij.tolk.ide

import com.intellij.openapi.actionSystem.IdeActions
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.replaceCaretMarker

abstract class TolkUpDownMoverTestBase : TolkTestBase() {
    protected fun moveDown(before: String, after: String = before) = doTest(
        before,
        after,
        IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION,
    )

    protected fun moveUp(before: String, after: String = before) = doTest(
        before,
        after,
        IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION,
    )

    private fun doTest(before: String, after: String, actionId: String) {
        myFixture.configureByText(
            "main.tolk",
            replaceCaretMarker(before.trimIndent() + "\n"),
        )
        myFixture.performEditorAction(actionId)
        myFixture.checkResult(
            replaceCaretMarker(after.trimIndent() + "\n"),
        )
    }
}
