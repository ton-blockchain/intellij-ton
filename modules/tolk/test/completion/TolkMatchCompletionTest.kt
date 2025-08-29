package org.ton.intellij.tolk.completion

class TolkMatchCompletionTest : TolkCompletionTestBase() {
    fun `test match arms completion for enum type`() = checkEquals(
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
        1,
        "color", "Color", "Color.Blue", "Color.Red", "else",
    )

    fun `test match arms completion for enum type with complete`() = doFirstCompletion(
        """
            enum Color {
                Red, Blue
            }
            
            fun foo(color: Color) {
                match (color) {
                    Re/*caret*/
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
                        /*caret*/
                    }
                }
            }
        """.trimIndent(),
    )

    fun `test match arms completion for enum type with partial expression`() = checkEquals(
        """
            enum Color {
                Red, Blue
            }
            
            fun foo(color: Color) {
                match (color) {
                    Color./*caret*/
                }
            }
        """,
        1,
        "Blue", "Red",
    )
}
