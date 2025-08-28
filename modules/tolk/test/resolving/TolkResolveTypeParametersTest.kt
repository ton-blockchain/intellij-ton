package org.ton.intellij.tolk.resolving

open class TolkResolveTypeParametersTest : TolkResolvingTestBase() {
    fun `test type parameter for struct`() {
        mainFile("main.tolk", """
            struct Foo<T> {
                a: /*caret*/T
            }
        """.trimIndent())

        assertReferencedTo("TYPE_PARAMETER:T T")
    }

    fun `test type parameter for function`() {
        mainFile("main.tolk", """
            fun foo<T>(): /*caret*/T {}
        """.trimIndent())

        assertReferencedTo("TYPE_PARAMETER:T T")
    }

    fun `test type parameter for function with same name as other type`() {
        mainFile("main.tolk", """
            struct TName {}
            
            fun foo<TName>(): /*caret*/TName {}
        """.trimIndent())

        assertReferencedTo("TYPE_PARAMETER:TName TName")
    }
}
