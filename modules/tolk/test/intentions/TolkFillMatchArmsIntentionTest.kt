package org.ton.intellij.tolk.intentions

import com.intellij.codeInsight.template.TemplateManager
import org.intellij.lang.annotations.Language
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.replaceCaretMarker

class TolkFillMatchArmsIntentionTest : TolkTestBase() {
    fun `test fills empty match over union`() = doAvailableTest(
        """
            struct AskToTransfer {}
            struct AskToBurn {}

            type Message = AskToTransfer | AskToBurn

            fun foo(msg: Message) {
                match (msg) {
                    /*caret*/
                }
            }
        """,
        """
            struct AskToTransfer {}
            struct AskToBurn {}

            type Message = AskToTransfer | AskToBurn

            fun foo(msg: Message) {
                match (msg) {
                    AskToTransfer => {

                    }
                    AskToBurn => {

                    }
                    else => {

                    }
                }
            }
        """,
    )

    fun `test fills only missing arms`() = doAvailableTest(
        """
            struct AskToTransfer {}
            struct AskToBurn {}

            type Message = AskToTransfer | AskToBurn

            fun foo(msg: Message) {
                match/*caret*/ (msg) {
                    AskToTransfer => {
                        return;
                    }
                }
            }
        """,
        """
            struct AskToTransfer {}
            struct AskToBurn {}

            type Message = AskToTransfer | AskToBurn

            fun foo(msg: Message) {
                match (msg) {
                    AskToTransfer => {
                        return;
                    }
                    AskToBurn => {

                    }
                    else => {

                    }
                }
            }
        """,
    )

    fun `test adds comma after expression arm`() = doAvailableTest(
        """
            struct AskToTransfer {}
            struct AskToBurn {}

            type Message = AskToTransfer | AskToBurn

            fun foo(msg: Message) {
                val x = match/*caret*/ (msg) {
                    AskToTransfer => 1
                };
            }
        """,
        """
            struct AskToTransfer {}
            struct AskToBurn {}

            type Message = AskToTransfer | AskToBurn

            fun foo(msg: Message) {
                val x = match (msg) {
                    AskToTransfer => 1,
                    AskToBurn => {

                    }
                    else => {

                    }
                };
            }
        """,
    )

    fun `test fills match over enum`() = doAvailableTest(
        """
            enum Color {
                Red, Blue
            }

            fun foo(color: Color) {
                match (color) {
                    /*caret*/
                }
            }
        """,
        """
            enum Color {
                Red, Blue
            }

            fun foo(color: Color) {
                match (color) {
                    Color.Red => {

                    }
                    Color.Blue => {

                    }
                    else => {

                    }
                }
            }
        """,
    )

    fun `test generated arms go in front of the else arm`() = doAvailableTest(
        """
            enum Color {
                Red, Blue
            }

            fun foo(color: Color) {
                match/*caret*/ (color) {
                    Color.Red => {
                    }
                    else => {
                    }
                }
            }
        """,
        """
            enum Color {
                Red, Blue
            }

            fun foo(color: Color) {
                match (color) {
                    Color.Red => {
                    }
                    Color.Blue => {

                    }
                    else => {
                    }
                }
            }
        """,
    )

    fun `test inserts at the caret when it stands above the arms`() = doAvailableTest(
        """
            struct AskToTransfer {}
            struct AskToBurn {}

            type Message = AskToTransfer | AskToBurn

            fun foo(msg: Message) {
                match (msg) {
                    /*caret*/
                    AskToTransfer => {
                        return;
                    }
                }
            }
        """,
        """
            struct AskToTransfer {}
            struct AskToBurn {}

            type Message = AskToTransfer | AskToBurn

            fun foo(msg: Message) {
                match (msg) {
                    AskToBurn => {

                    }
                    AskToTransfer => {
                        return;
                    }
                    else => {

                    }
                }
            }
        """,
    )

    fun `test inserts at the caret between the arms`() = doAvailableTest(
        """
            enum Color {
                Red, Green, Blue
            }

            fun foo(color: Color) {
                match (color) {
                    Color.Red => {
                    }
                    /*caret*/
                    Color.Blue => {
                    }
                }
            }
        """,
        """
            enum Color {
                Red, Green, Blue
            }

            fun foo(color: Color) {
                match (color) {
                    Color.Red => {
                    }
                    Color.Green => {

                    }
                    Color.Blue => {
                    }
                    else => {

                    }
                }
            }
        """,
    )

    fun `test preview shows the generated arms`() = doPreviewTest(
        """
            struct AskToTransfer {}
            struct AskToBurn {}

            type Message = AskToTransfer | AskToBurn

            fun foo(msg: Message) {
                match (msg) {
                    /*caret*/
                }
            }
        """,
        """
            struct AskToTransfer {}
            struct AskToBurn {}

            type Message = AskToTransfer | AskToBurn

            fun foo(msg: Message) {
                match (msg) {
                    AskToTransfer => {

                    }
                    AskToBurn => {

                    }
                    else => {

                    }
                }
            }
        """,
    )

    fun `test preview follows the caret`() = doPreviewTest(
        """
            enum Color {
                Red, Green, Blue
            }

            fun foo(color: Color) {
                match (color) {
                    Color.Red => {
                    }
                    /*caret*/
                    Color.Blue => {
                    }
                }
            }
        """,
        """
            enum Color {
                Red, Green, Blue
            }

            fun foo(color: Color) {
                match (color) {
                    Color.Red => {
                    }
                    Color.Green => {

                    }
                    Color.Blue => {
                    }
                    else => {

                    }
                }
            }
        """,
    )

    fun `test a generic variant named without type arguments is already covered`() = doUnavailableTest(
        """
            struct Ok<T> { result: T }
            struct Err<T> { errPayload: T }

            type Response<TResult, TError> = Ok<TResult> | Err<TError>

            fun foo(r: Response<int, slice>) {
                match/*caret*/ (r) {
                    Ok => {
                    }
                    Err => {
                    }
                    else => {
                    }
                }
            }
        """,
    )

    fun `test an aliased variant is already covered`() = doUnavailableTest(
        """
            struct Ok<T> { result: T }
            struct Err<T> { errPayload: T }

            type OkAlias<T> = Ok<T>

            type Response<TResult, TError> = Ok<TResult> | Err<TError>

            fun foo(r: Response<int, slice>) {
                match/*caret*/ (r) {
                    OkAlias<int> => {
                    }
                    Err<slice> => {
                    }
                    else => {
                    }
                }
            }
        """,
    )

    fun `test fills only the generic variant that is not covered`() = doAvailableTest(
        """
            struct Ok<T> { result: T }
            struct Err<T> { errPayload: T }

            type Response<TResult, TError> = Ok<TResult> | Err<TError>

            fun foo(r: Response<int, slice>) {
                match/*caret*/ (r) {
                    Ok => {
                    }
                }
            }
        """,
        """
            struct Ok<T> { result: T }
            struct Err<T> { errPayload: T }

            type Response<TResult, TError> = Ok<TResult> | Err<TError>

            fun foo(r: Response<int, slice>) {
                match (r) {
                    Ok => {
                    }
                    Err<slice> => {

                    }
                    else => {

                    }
                }
            }
        """,
    )

    fun `test not available when all arms are declared`() = doUnavailableTest(
        """
            struct AskToTransfer {}
            struct AskToBurn {}

            type Message = AskToTransfer | AskToBurn

            fun foo(msg: Message) {
                match/*caret*/ (msg) {
                    AskToTransfer => {
                    }
                    AskToBurn => {
                    }
                    else => {
                    }
                }
            }
        """,
    )

    fun `test not available inside an arm body`() = doUnavailableTest(
        """
            struct AskToTransfer {}
            struct AskToBurn {}

            type Message = AskToTransfer | AskToBurn

            fun foo(msg: Message) {
                match (msg) {
                    AskToTransfer => {
                        /*caret*/
                    }
                }
            }
        """,
    )

    fun `test not available for a non-union match`() = doUnavailableTest(
        """
            fun foo(x: int) {
                match (x) {
                    /*caret*/
                }
            }
        """,
    )

    private fun doAvailableTest(@Language("Tolk") before: String, @Language("Tolk") after: String) {
        myFixture.configureByText("test.tolk", replaceCaretMarker(before.trimIndent()))

        myFixture.launchAction(myFixture.findSingleIntention(INTENTION_NAME))

        val templateManager = TemplateManager.getInstance(project)
        if (templateManager.getActiveTemplate(myFixture.editor) != null) {
            templateManager.finishTemplate(myFixture.editor)
        }

        myFixture.checkResult(after.trimIndent(), true)
    }

    private fun doPreviewTest(@Language("Tolk") before: String, @Language("Tolk") after: String) {
        myFixture.configureByText("test.tolk", replaceCaretMarker(before.trimIndent()))

        val preview = myFixture.getIntentionPreviewText(myFixture.findSingleIntention(INTENTION_NAME))

        assertEquals(after.trimIndent(), preview?.trimEnd()?.lines()?.joinToString("\n") { it.trimEnd() })
    }

    private fun doUnavailableTest(@Language("Tolk") before: String) {
        myFixture.configureByText("test.tolk", replaceCaretMarker(before.trimIndent()))
        assertEmpty(myFixture.filterAvailableIntentions(INTENTION_NAME))
    }

    companion object {
        private const val INTENTION_NAME = "Fill all cases"
    }
}
