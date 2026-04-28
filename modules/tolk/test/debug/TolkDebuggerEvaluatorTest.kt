package org.ton.intellij.tolk.debug

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.xdebugger.evaluation.ExpressionInfo
import org.ton.intellij.tolk.TolkTestBase

class TolkDebuggerEvaluatorTest : TolkTestBase() {
    fun testFindExpressionInfoForReferenceExpression() {
        val info = configureAndFindExpressionInfo(
            """
                fun main() {
                    val subjectValue = 1
                    val result = subjectVal<caret>ue
                }
            """.trimIndent(),
        )

        assertExpressionInfo(info, "subjectValue")
    }

    fun testFindExpressionInfoForVariableDefinitionIdentifier() {
        val info = configureAndFindExpressionInfo(
            """
                fun main() {
                    val subjectVal<caret>ue = 1
                    val result = subjectValue
                }
            """.trimIndent(),
        )

        assertExpressionInfo(info, "subjectValue")
    }

    fun testFindExpressionInfoForParameterDefinitionIdentifier() {
        val info = configureAndFindExpressionInfo(
            """
                fun main(subjectVal<caret>ue: int) {
                    val result = subjectValue
                }
            """.trimIndent(),
        )

        assertExpressionInfo(info, "subjectValue")
    }

    fun testFindExpressionInfoForFieldLookupUsesWholeDotExpression() {
        val info = configureAndFindExpressionInfo(
            """
                fun main(item: Item) {
                    val result = item.fieldNam<caret>e
                }
            """.trimIndent(),
        )

        assertExpressionInfo(info, "item.fieldName")
    }

    fun testFindExpressionInfoForCallCalleeReturnsNull() {
        val info = configureAndFindExpressionInfo(
            """
                fun helper(): int = 1

                fun main() {
                    val result = helpe<caret>r()
                }
            """.trimIndent(),
        )

        assertNull(info)
    }

    fun testFindExpressionInfoReturnsNullForUncommittedDocument() {
        myFixture.configureByText(
            "main.tolk",
            """
                fun main() {
                    val subjectValue = 1
                    val result = subjectVal<caret>ue
                }
            """.trimIndent(),
        )
        val document = myFixture.editor.document

        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(document.textLength, "\n")
        }

        assertFalse(PsiDocumentManager.getInstance(project).isCommitted(document))
        assertNull(findExpressionInfoAtOffset(project, document, myFixture.caretOffset))
    }

    private fun configureAndFindExpressionInfo(code: String): ExpressionInfo? {
        myFixture.configureByText("main.tolk", code)
        return findExpressionInfoAtOffset(project, myFixture.editor.document, myFixture.caretOffset)
    }

    private fun assertExpressionInfo(info: ExpressionInfo?, expectedExpression: String) {
        assertNotNull(info)
        assertEquals(expectedExpression, info!!.expressionText)
        assertEquals(expectedExpression, info.displayText)
        assertEquals(
            expectedExpression,
            myFixture.file.text.substring(info.textRange.startOffset, info.textRange.endOffset),
        )
    }
}
