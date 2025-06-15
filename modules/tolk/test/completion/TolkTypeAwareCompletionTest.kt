package org.ton.intellij.tolk.completion

class TolkTypeAwareCompletionTest : TolkCompletionTestBase() {
    fun `test static function call`() = doSingleCompletion("""
        struct S;
        fun S.someStaticFunction() {}
        fun main() { S.so/*caret*/ }
    """, """
        struct S;
        fun S.someStaticFunction() {}
        fun main() { S.someStaticFunction()/*caret*/ }
    """)

    fun `test static function call with arg`() = doSingleCompletion("""
        struct S;
        fun S.someStaticFunctionArg(foo: int) {}
        fun main() { S.so/*caret*/ }
    """, """
        struct S;
        fun S.someStaticFunctionArg(foo: int) {}
        fun main() { S.someStaticFunctionArg(/*caret*/) }
    """)

    fun `test don't suggest fields for methods`() = doSingleCompletion("""
        struct S { transmogrificator: int }
        fun S.transmogrify(self) {}
        fun main(s: S) { s.trans/*caret*/() }
    """, """
        struct S { transmogrificator: int }
        fun S.transmogrify(self) {}
        fun main(s: S) { s.transmogrify()/*caret*/ }
    """.trimIndent())

    fun `test method call on self`() = doSingleCompletion("""
        struct S;
        fun S.someMethod(self) {}
        fun S.someStaticFunction() {}
        fun main(s: S) { s.someMe/*caret*/ }
    """, """
        struct S;
        fun S.someMethod(self) {}
        fun S.someStaticFunction() {}
        fun main(s: S) { s.someMethod()/*caret*/ }
    """)

    fun `test method on self and arg`() = doSingleCompletion("""
        struct S;
        fun S.someMethodArg(self, foo: int) {}
        fun S.someStaticFunction() {}
        fun S.someStaticFunctionArg(foo: int) {}
        fun main(s: S) { s.someMe/*caret*/ }
    """, """
        struct S;
        fun S.someMethodArg(self, foo: int) {}
        fun S.someStaticFunction() {}
        fun S.someStaticFunctionArg(foo: int) {}
        fun main(s: S) { s.someMethodArg(/*caret*/) }
    """)

    fun `test field expr`() = doSingleCompletion("""
        struct S { transmogrificator: int }
        fun main() {
            var s = S { transmogrificator: 42 }
            s.trans/*caret*/
        }
    """, """
        struct S { transmogrificator: int }
        fun main() {
            var s = S { transmogrificator: 42 }
            s.transmogrificator/*caret*/
        }
    """)

    fun `test static function`() = doSingleCompletion("""
        struct S;
        fun S.create(): S { S {} }
        fun main() { val _ = S.cr/*caret*/; }
    """, """
        struct S;
        fun S.create(): S { S {} }
        fun main() { val _ = S.create()/*caret*/; }
    """)

    fun `test self method`() = doSingleCompletion("""
        struct Foo;
        fun Foo.frobnicate(self) {}
        fun Foo.bar(self) { self.frob/*caret*/ }
    """, """
        struct Foo;
        fun Foo.frobnicate(self) {}
        fun Foo.bar(self) { self.frobnicate()/*caret*/ }
    """)

    fun `test no completion for a method with self`() = checkNoCompletion("""
        struct S;
        fun S.foo(self) {}
        fun main() { S.fo/*caret*/; }
    """.trimIndent())
}
