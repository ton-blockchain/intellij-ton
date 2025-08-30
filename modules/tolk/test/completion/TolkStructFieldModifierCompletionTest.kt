package org.ton.intellij.tolk.completion

class TolkStructFieldModifierCompletionTest : TolkCompletionTestBase() {
    
    fun `test private modifier completion in struct body`() = checkContainsCompletion(
        "private",
        """
            struct MyStruct {
                priv/*caret*/
            }
        """.trimIndent()
    )
    
    fun `test readonly modifier completion in struct body`() = checkContainsCompletion(
        "readonly",
        """
            struct MyStruct {
                read/*caret*/
            }
        """.trimIndent()
    )
    
    fun `test both modifiers available in struct body`() = checkContainsCompletion(
        listOf("private", "readonly"),
        """
            struct MyStruct {
                /*caret*/
            }
        """.trimIndent()
    )
    
    fun `test private modifier completion after typing pr`() = doSingleCompletion(
        """
            struct MyStruct {
                pr/*caret*/
            }
        """,
        """
            struct MyStruct {
                private /*caret*/
            }
        """
    )
    
    fun `test readonly modifier completion after typing re`() = doSingleCompletion(
        """
            struct MyStruct {
                re/*caret*/
            }
        """,
        """
            struct MyStruct {
                readonly /*caret*/
            }
        """
    )
    
    fun `test private completion and field definition`() = doFirstCompletion(
        """
            struct MyStruct {
                priv/*caret*/ field: int;
            }
        """,
        """
            struct MyStruct {
                private /*caret*/ field: int;
            }
        """
    )
    
    fun `test readonly completion and field definition`() = doFirstCompletion(
        """
            struct MyStruct {
                read/*caret*/ field: int;
            }
        """,
        """
            struct MyStruct {
                readonly /*caret*/ field: int;
            }
        """
    )
    
    fun `test modifiers not available after field name`() = checkNotContainsCompletion(
        listOf("private", "readonly"),
        """
            struct MyStruct {
                field: /*caret*/
            }
        """.trimIndent()
    )
    
    fun `test modifiers not available after colon`() = checkNotContainsCompletion(
        listOf("private", "readonly"),
        """
            struct MyStruct {
                field: int/*caret*/
            }
        """.trimIndent()
    )
    
    fun `test modifiers not available outside struct`() = checkNotContainsCompletion(
        listOf("private", "readonly"),
        """
            priv/*caret*/
            struct MyStruct {
                field: int;
            }
        """.trimIndent()
    )
    
    fun `test modifiers not available in function`() = checkNotContainsCompletion(
        listOf("private", "readonly"),
        """
            fun test() {
                priv/*caret*/
            }
        """.trimIndent()
    )
    
    fun `test multiple modifiers completion`() = doFirstCompletion(
        """
            struct MyStruct {
                private read/*caret*/
            }
        """,
        """
            struct MyStruct {
                private readonly /*caret*/
            }
        """
    )
    
    fun `test private after readonly completion`() = doFirstCompletion(
        """
            struct MyStruct {
                readonly priv/*caret*/
            }
        """,
        """
            struct MyStruct {
                readonly private /*caret*/
            }
        """
    )

    fun `test modifiers in nested struct`() = checkContainsCompletion(
        listOf("private", "readonly"),
        """
            struct Outer {
                field: Inner;
            }
            
            struct Inner {
                /*caret*/
            }
        """.trimIndent()
    )
    
    fun `test modifiers with multiple fields`() = checkContainsCompletion(
        listOf("private", "readonly"),
        """
            struct MyStruct {
                field1: int;
                private field2: string;
                /*caret*/
            }
        """.trimIndent()
    )

    fun `test readonly modifier completion priority`() = checkOrderedEquals(
        """
            struct MyStruct {
                r/*caret*/
            }
        """.trimIndent(),
        1,
        "readonly",
        "private",
    )
}
