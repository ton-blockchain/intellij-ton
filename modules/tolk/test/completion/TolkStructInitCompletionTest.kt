package org.ton.intellij.tolk.completion

import org.ton.intellij.tolk.replaceCaretMarker

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

    fun `test field completion replaces existing field name`() = checkFieldNameReplacement('\n')

    fun `test field completion replaces existing field name with replace selection`() = checkFieldNameReplacement('\t')

    fun `test coins completion prefers grams over the deprecated ton alias`() {
        myFixture.addFileToProject(
            ".acton/tolk-stdlib/common.tolk",
            """
            fun grams(value: string): coins builtin
            fun ton(value: string): coins builtin
            """.trimIndent(),
        )

        checkCoinsCompletion("grams")
    }

    fun `test coins completion keeps ton with an older stdlib`() {
        myFixture.addFileToProject(".acton/tolk-stdlib/common.tolk", "fun ton(value: slice): coins builtin")

        checkCoinsCompletion("ton")
    }

    fun `test coins completion defaults to grams without a stdlib`() = checkCoinsCompletion("grams")

    fun `test coins completion uses the nested project stdlib`() {
        myFixture.addFileToProject(".acton/tolk-stdlib/common.tolk", "fun ton(value: slice): coins builtin")
        myFixture.addFileToProject("packages/app/Acton.toml", "")
        myFixture.addFileToProject(
            "packages/app/.acton/tolk-stdlib/common.tolk",
            "fun grams(value: string): coins builtin",
        )

        val fixture = object : TolkCompletionTestFixtureBase<String>(myFixture) {
            override fun prepare(code: String) {
                val file = myFixture.addFileToProject("packages/app/main.tolk", replaceCaretMarker(code.trimIndent()))
                myFixture.configureFromExistingVirtualFile(file.virtualFile)
            }
        }
        fixture.apply {
            setUp()
            try {
                checkCoinsCompletion("grams", this)
            } finally {
                tearDown()
            }
        }
    }

    fun `test fill all fields uses grams for nested coins values`() {
        myFixture.addFileToProject(".acton/tolk-stdlib/common.tolk", "fun grams(value: string): coins builtin")

        checkCompletion(
            "0",
            """
                struct Cell<T> {}
                struct Transfer {
                    amount: coins
                    tuple: [coins, int]
                    tensor: (coins, bool)
                    cell: Cell<coins>
                }

                fun main() {
                    Transfer {
                        /*caret*/
                    };
                }
            """,
            """
                struct Cell<T> {}
                struct Transfer {
                    amount: coins
                    tuple: [coins, int]
                    tensor: (coins, bool)
                    cell: Cell<coins>
                }

                fun main() {
                    Transfer {
                        amount: grams("0.1"),
                        tuple: [grams("0.1"), 0],
                        tensor: (grams("0.1"), false),
                        cell: grams("0.1").toCell(),/*caret*/
                    };
                }
            """,
        )
    }

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

    fun `test field completion with map type uses bracket literal`() = checkCompletion(
        "balances",
        """
            struct map<K, V> {}
            
            struct Foo {
                balances: map<int, int>
            }

            fun foo() {
                Foo { bala/*caret*/ };
            }
        """,
        """
            struct map<K, V> {}
            
            struct Foo {
                balances: map<int, int>
            }

            fun foo() {
                Foo { balances: []/*caret*/ };
            }
        """.trimIndent(),
        '\t',
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
            
            fun Foo.create() {
                return Foo { /*caret*/ };
            }
        """,
        1,
        "foo",
    )

    private fun checkCoinsCompletion(
        functionName: String,
        fixture: TolkCompletionTestFixtureBase<String> = completionFixture,
    ) = fixture.checkCompletion(
        "amount",
        """
            struct Transfer {
                amount: coins
            }

            fun main() {
                Transfer { am/*caret*/ };
            }
        """,
        """
            struct Transfer {
                amount: coins
            }

            fun main() {
                Transfer { amount: $functionName("0.1")/*caret*/ };
            }
        """,
        '\n',
    )

    private fun checkFieldNameReplacement(completionChar: Char) = checkCompletion(
        "bbbb",
        """
            struct Foo {
                a: int,
                bbbb: int,
            }

            fun main() {
                Foo {
                    /*caret*/a: 100
                }
            }
        """,
        """
            struct Foo {
                a: int,
                bbbb: int,
            }

            fun main() {
                Foo {
                    bbbb/*caret*/: 100
                }
            }
        """.trimIndent(),
        completionChar,
    )
}
