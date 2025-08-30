package org.ton.intellij.tolk.completion

class TolkEnumMemberCompletionTest : TolkCompletionTestBase() {
    fun `test enum member completion without qualifier`() = doFirstCompletion(
        """
            enum Color {
                Red, Blue
            }
            
            fun foo() {
                Red/*caret*/
            }
        """,
        """
            enum Color {
                Red, Blue
            }
            
            fun foo() {
                Color.Red/*caret*/
            }
        """.trimIndent()
    )

    fun `test enum member completion without qualifier in assignment`() = doFirstCompletion(
        """
            enum Color {
                Red, Blue
            }
            
            fun foo() {
                val a = Color.Red;
                a = Red/*caret*/
            }
        """,
        """
            enum Color {
                Red, Blue
            }
            
            fun foo() {
                val a = Color.Red;
                a = Color.Red/*caret*/
            }
        """.trimIndent()
    )

    fun `test enum member completion with qualifier`() = doFirstCompletion(
        """
            enum Color {
                Red, Blue
            }
            
            fun foo() {
                Color.Re/*caret*/
            }
        """,
        """
            enum Color {
                Red, Blue
            }
            
            fun foo() {
                Color.Red/*caret*/
            }
        """.trimIndent()
    )
}
