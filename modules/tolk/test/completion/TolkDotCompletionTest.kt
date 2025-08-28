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
}
