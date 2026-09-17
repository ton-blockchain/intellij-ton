package org.ton.intellij.tolk.intentions

import com.intellij.codeInsight.template.TemplateManager
import org.intellij.lang.annotations.Language
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.replaceCaretMarker

class TolkFillStructFieldsIntentionTest : TolkTestBase() {
    fun `test fills an empty initializer`() = doAvailableTest(
        """
            struct Point {
                x: int
                y: int
            }

            fun main() {
                var p = Point {/*caret*/};
            }
        """,
        """
            struct Point {
                x: int
                y: int
            }

            fun main() {
                var p = Point {
                    x: 0,
                    y: 0,
                };
            }
        """,
    )

    fun `test fills only the missing fields`() = doAvailableTest(
        """
            struct Point {
                x: int
                y: int
                flag: bool
            }

            fun main() {
                var p = Point/*caret*/ {
                    x: 10,
                };
            }
        """,
        """
            struct Point {
                x: int
                y: int
                flag: bool
            }

            fun main() {
                var p = Point {
                    x: 10,
                    y: 0,
                    flag: false,
                };
            }
        """,
    )

    fun `test adds a comma after the last field`() = doAvailableTest(
        """
            struct Point {
                x: int
                y: int
            }

            fun main() {
                var p = Point/*caret*/ {
                    x: 10
                };
            }
        """,
        """
            struct Point {
                x: int
                y: int
            }

            fun main() {
                var p = Point {
                    x: 10,
                    y: 0,
                };
            }
        """,
    )

    fun `test inserts at the caret between the fields`() = doAvailableTest(
        """
            struct Point {
                x: int
                y: int
                flag: bool
            }

            fun main() {
                var p = Point {
                    x: 10,
                    /*caret*/
                    flag: true,
                };
            }
        """,
        """
            struct Point {
                x: int
                y: int
                flag: bool
            }

            fun main() {
                var p = Point {
                    x: 10,
                    y: 0,
                    flag: true,
                };
            }
        """,
    )

    fun `test uses default values matching the field types`() = doAvailableTest(
        """
            struct Wallet {
                balance: coins
                owner: address
                code: cell
                name: slice
                active: bool
            }

            fun main() {
                var w = Wallet {/*caret*/};
            }
        """,
        """
            struct Wallet {
                balance: coins
                owner: address
                code: cell
                name: slice
                active: bool
            }

            fun main() {
                var w = Wallet {
                    balance: grams("0.1"),
                    owner: address(""),
                    code: createEmptyCell(),
                    name: createEmptySlice(),
                    active: false,
                };
            }
        """,
    )

    fun `test fills required fields only`() = doAvailableTest(
        """
            struct Point {
                x: int
                y: int
                flag: bool = true
            }

            fun main() {
                var p = Point {/*caret*/};
            }
        """,
        """
            struct Point {
                x: int
                y: int
                flag: bool = true
            }

            fun main() {
                var p = Point {
                    x: 0,
                    y: 0,
                };
            }
        """,
        REQUIRED_INTENTION,
    )

    fun `test preview shows the generated fields`() = doPreviewTest(
        """
            struct Point {
                x: int
                y: int
            }

            fun main() {
                var p = Point {/*caret*/};
            }
        """,
        """
            struct Point {
                x: int
                y: int
            }

            fun main() {
                var p = Point {
                    x: 0,
                    y: 0,
                };
            }
        """,
    )

    fun `test not available when every field is initialized`() = doUnavailableTest(
        """
            struct Point {
                x: int
                y: int
            }

            fun main() {
                var p = Point/*caret*/ {
                    x: 1,
                    y: 2,
                };
            }
        """,
    )

    fun `test not available inside a field value`() = doUnavailableTest(
        """
            struct Point {
                x: int
                y: int
            }

            fun main() {
                var p = Point {
                    x: 1 + /*caret*/2,
                };
            }
        """,
    )

    fun `test required variant is hidden when it fills everything`() = doUnavailableTest(
        """
            struct Point {
                x: int
                y: int
            }

            fun main() {
                var p = Point {/*caret*/};
            }
        """,
        REQUIRED_INTENTION,
    )

    private fun doAvailableTest(
        @Language("Tolk") before: String,
        @Language("Tolk") after: String,
        intention: String = ALL_INTENTION,
    ) {
        myFixture.configureByText("test.tolk", replaceCaretMarker(before.trimIndent()))

        myFixture.launchAction(myFixture.findSingleIntention(intention))

        val templateManager = TemplateManager.getInstance(project)
        if (templateManager.getActiveTemplate(myFixture.editor) != null) {
            templateManager.finishTemplate(myFixture.editor)
        }

        myFixture.checkResult(after.trimIndent(), true)
    }

    private fun doPreviewTest(
        @Language("Tolk") before: String,
        @Language("Tolk") after: String,
        intention: String = ALL_INTENTION,
    ) {
        myFixture.configureByText("test.tolk", replaceCaretMarker(before.trimIndent()))

        val preview = myFixture.getIntentionPreviewText(myFixture.findSingleIntention(intention))

        assertEquals(after.trimIndent(), preview?.trimEnd()?.lines()?.joinToString("\n") { it.trimEnd() })
    }

    private fun doUnavailableTest(@Language("Tolk") before: String, intention: String = ALL_INTENTION) {
        myFixture.configureByText("test.tolk", replaceCaretMarker(before.trimIndent()))
        assertEmpty(myFixture.filterAvailableIntentions(intention))
    }

    companion object {
        private const val ALL_INTENTION = "Fill all fields"
        private const val REQUIRED_INTENTION = "Fill required fields"
    }
}
