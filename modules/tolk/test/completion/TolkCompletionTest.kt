package org.ton.intellij.tolk.completion

class TolkCompletionTest : TolkCompletionTestBase() {
    fun `test local variable`() = doSingleCompletion("""
        fun foo(quux: int) { qu/*caret*/ }
    """, """
        fun foo(quux: int) { quux/*caret*/ }
    """)

    fun `test function call zero args`() = doSingleCompletion("""
        fun foo() {}
        fun main() { fo/*caret*/ }
    """, """
        fun foo() {}
        fun main() { foo()/*caret*/ }
    """)

    fun `test function call one arg`() = doSingleCompletion("""
        fun foo(x: int) {}
        fun main() { fo/*caret*/ }
    """, """
        fun foo(x: int) {}
        fun main() { foo(/*caret*/) }
    """)

    fun `test function call with parens`() = doSingleCompletion("""
        fun foo() {}
        fun main() { fo/*caret*/() }
    """, """
        fun foo() {}
        fun main() { foo()/*caret*/ }
    """)

    fun `test function call with parens with arg`() = doSingleCompletion("""
        fun foo(x: int) {}
        fun main() { fo/*caret*/() }
    """, """
        fun foo(x: int) {}
        fun main() { foo(/*caret*/) }
    """)

    fun `test function call with parens overwrite`() = doSingleCompletion("""
        fun foo(x: int) {}
        fun main() { fo/*caret*/transmog() }
    """, """
        fun foo(x: int) {}
        fun main() { foo(/*caret*/)transmog() }
    """)

    fun `test local scope`() = checkNoCompletion("""
        fun main() {
            val x = spam/*caret*/;
            val spamlot = 42;
        }
    """.trimIndent())

    fun `test tuple field completion`() = checkContainsCompletion("1", """
        fun main() {
            val x = (0, 1);
            x./*caret*/
        }
    """.trimIndent())

    fun `test completion after tuple field expr`() = doSingleCompletion("""
        struct S { field: int }
        fun main() {
            val x = (0, S { field: 0 });
            x.1./*caret*/
        }
    """, """
        struct S { field: int }
        fun main() {
            val x = (0, S { field: 0 });
            x.1.field/*caret*/
        }
    """)

//    fun `test caret navigation in self method`() = doSingleCompletion("""
//        struct Foo;
//        fun Foo.foo(self) {}
//        fun main() { Foo.fo/*caret*/ }
//    """, """
//        struct Foo;
//        fun Foo.foo(self) {}
//        fun main() { Foo.foo(/*caret*/) }
//    """.trimIndent())
}
