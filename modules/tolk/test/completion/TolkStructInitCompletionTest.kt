package org.ton.intellij.tolk.completion

class TolkStructInitCompletionTest : TolkCompletionTestBase() {
    fun `test field completion with enum type`() = doFirstCompletion(
        """
            enum Color {
                Red, Blue
            }
            
            struct Foo {
                color: Color
            }
            
            fun foo() {
                Foo { colo/*caret*/ };
            }
        """,
        """
            enum Color {
                Red, Blue
            }
            
            struct Foo {
                color: Color
            }
            
            fun foo() {
                Foo { color: Color.Red/*caret*/ };
            }
        """.trimIndent(),
    )

    fun `test field completion with empty enum type`() = doFirstCompletion(
        """
            enum Color {}
            
            struct Foo {
                color: Color
            }
            
            fun foo() {
                Foo { colo/*caret*/ };
            }
        """,
        """
            enum Color {}
            
            struct Foo {
                color: Color
            }
            
            fun foo() {
                Foo { color: Color/*caret*/ };
            }
        """.trimIndent(),
    )
}