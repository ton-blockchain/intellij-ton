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
        "Red", "Blue", "foo"
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
        "Red", "Blue", "foo"
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
        "foo"
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

    fun `test struct fields completion`() = checkOrderedEquals(
        """
            struct Foo {
                value: int
            }
            
            fun foo() {
                val f: Foo;
                f./*caret*/
            }
        """,
        1,
        "value",
        "foo",
    )

    fun `test simple instance method completion`() = checkOrderedEquals(
        """
            struct Foo {}
            
            fun Foo.method1(self) {}
            fun Foo.method2(self) {}
            fun Foo.method3(self) {}
            
            fun foo() {
                val f: Foo;
                f./*caret*/
            }
        """,
        1,
        "method1",
        "method2",
        "method3",
    )

    fun `test don't add static methods on instance method completion`() = checkOrderedEquals(
        """
            struct Foo {}
            
            fun Foo.method1(self) {}
            fun Foo.static_method() {}
            
            fun foo() {
                val f: Foo;
                f./*caret*/
            }
        """,
        1,
        "method1",
    )

    fun `test simple static method completion`() = checkOrderedEquals(
        """
            struct Foo {}
            
            fun Foo.method1() {}
            fun Foo.method2() {}
            fun Foo.method3() {}
            
            fun foo() {
                Foo./*caret*/
            }
        """,
        1,
        "method1",
        "method2",
        "method3",
    )

    fun `test methods completion with static receiver`() = checkOrderedEquals(
        """
            struct Foo {}
            
            fun Foo.method1(self) {}
            fun Foo.method2(self) {}
            fun Foo.method3(self) {}
            fun Foo.method4() {} // static
            
            fun foo() {
                Foo./*caret*/
            }
        """,
        1,
        "method4", // should be first!
        "method1",
        "method2",
        "method3",
    )

    fun `test instance method completion on generic struct`() = checkOrderedEquals(
        """
            struct Foo<T> {}
            
            fun Foo<T>.method1(self) {}
            fun Foo<T>.method2(self) {}
            fun Foo<T>.method3(self) {}
            
            fun foo() {
                val f: Foo<int>;
                f./*caret*/
            }
        """,
        1,
        "method1",
        "method2",
        "method3",
    )

    fun `test instance method completion on generic struct with specializations`() = checkOrderedEquals(
        """
            struct Foo<T> {}
            
            fun Foo<T>.method1(self) {}
            fun Foo<slice>.method2(self) {}
            fun Foo<int>.method3(self) {}
            
            fun foo() {
                val f: Foo<int>;
                f./*caret*/
            }
        """,
        1,
        "method3",
        "method1",
    )

    fun `test static method completion on generic struct`() = checkOrderedEquals(
        """
            struct Foo<T> {}
            
            fun Foo<T>.method1() {}
            fun Foo<T>.method2() {}
            fun Foo<T>.method3() {}
            
            fun foo() {
                Foo<int>./*caret*/
            }
        """,
        1,
        "method1",
        "method2",
        "method3",
    )

    fun `test static method completion on generic struct with specializations`() = checkOrderedEquals(
        """
            struct Foo<T> {}
            
            fun Foo<T>.method1() {}
            fun Foo<slice>.method2() {}
            fun Foo<int>.method3() {}
            
            fun foo() {
                Foo<int>./*caret*/
            }
        """,
        1,
        "method3",
        "method1",
    )
}
