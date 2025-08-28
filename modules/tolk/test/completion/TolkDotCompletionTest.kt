package org.ton.intellij.tolk.completion

class TolkDotCompletionTest : TolkCompletionTestBase() {
    fun `test enum completion`() = checkEquals(
        """
            enum Foo {
                Red, Blue
            }
            
            fun foo() {
                Foo./*caret*/
            }
        """,
        1,
        "Red", "Blue",
    )

    fun `test enum completion with static method`() = checkEquals(
        """
            enum Color {
                Red, Blue
            }
            
            fun Color.max(): Color {}
            
            fun foo() {
                Color./*caret*/
            }
        """,
        1,
        "Red", "Blue", "max",
    )

    fun `test enum completion via alias`() = checkEquals(
        """
            enum Foo {
                Red, Blue
            }

            type FooAlias = Foo
            
            fun foo() {
                FooAlias./*caret*/
            }
        """,
        1,
        "Red", "Blue",
    )

    fun `test enum completion via instance without methods`() = checkEquals(
        """
            enum Color {
                Red, Blue
            }

            fun foo(color: Color) {
                color./*caret*/
            }
        """,
        1,
    )

    fun `test enum completion via instance with instance method`() = checkEquals(
        """
            enum Color {
                Red, Blue
            }
            
            fun Color.isRed(self): bool {}

            fun foo(color: Color) {
                color./*caret*/
            }
        """,
        1,
        "isRed",
    )

    fun `test enum completion via instance with static method`() = checkEquals(
        """
            enum Color {
                Red, Blue
            }
            
            fun Color.max(): Color {}

            fun foo(color: Color) {
                color./*caret*/
            }
        """,
        1,
    )
}
