package org.ton.intellij.tolk.completion

class TolkStructInitCompletionTest : TolkCompletionTestBase() {
    fun `test field completion inside empty struct instance`() = checkEquals(
        """
            struct Foo {
                a: int,
                b: int,
            }
            
            fun main() {
                Foo {
                    /*caret*/
                }
            }
        """.trimIndent(),
        1,
        "0", // Fill all fields...
        "a",
        "b",
    )

    fun `test field completion inside struct instance with single field`() = checkEquals(
        """
            struct Foo {
                a: int,
                b: int,
            }
            
            fun main() {
                Foo {
                    a: 10,
                    /*caret*/
                }
            }
        """.trimIndent(),
        1,
        "b",
    )

    fun `test field completion inside struct instance with all fields`() = checkEquals(
        """
            struct Foo {
                a: int,
                b: int,
            }
            
            fun main() {
                Foo {
                    a: 10,
                    b: 20,
                    /*caret*/
                }
            }
        """.trimIndent(),
        1,
    )

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

    fun `test private field completion in function`() = checkEquals(
        """
            struct Foo {
                private foo: int
            }
            
            fun foo() {
                Foo { fo/*caret*/ };
            }
        """,
        1,
    )

    fun `test private field completion in struct method`() = checkEquals(
        """
            struct Foo {
                private foo: int
            }
            
            fun Foo.create(self) {
                return Foo { fo/*caret*/ };
            }
        """,
        1,
        "foo"
    )
}
