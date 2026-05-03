package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.eval.TolkEnumValueEvaluator
import org.ton.intellij.tolk.psi.TolkEnum
import org.ton.intellij.tolk.psi.impl.members

class TolkEnumValueEvaluatorTest : TolkTestBase() {
    fun `test sequential and explicit enum values`() {
        val values = enumValues(
            """
            enum Color {
                Red
                Green = 5
                Blue
                Negative = -1
                Next
            }
            """.trimIndent(),
        )

        assertEquals(
            mapOf(
                "Red" to "0",
                "Green" to "5",
                "Blue" to "6",
                "Negative" to "-1",
                "Next" to "0",
            ),
            values,
        )
    }

    fun `test enum values from constant expressions and other enum members`() {
        val values = enumValues(
            """
            const BASE = 10;

            enum Other {
                Item = 3
            }

            enum Color {
                Red = BASE + 1
                Green
                Blue = Other.Item + 2
                Yellow
            }
            """.trimIndent(),
            enumName = "Color",
        )

        assertEquals(
            mapOf(
                "Red" to "11",
                "Green" to "12",
                "Blue" to "5",
                "Yellow" to "6",
            ),
            values,
        )
    }

    fun `test enum values from arithmetic bitwise shifts and comparisons`() {
        val values = enumValues(
            """
            const BASE = 7;

            enum Source {
                One = 16
                Two = 8
            }

            enum Flags {
                A = (BASE + 1) * 2
                B = Source.One >> 1
                C = Source.Two << 2
                D = (Source.Two << 2) & 10
                E = ((Source.Two << 2) & 10) | 1
                F = (((Source.Two << 2) & 10) | 1) ^ 3
                G = ((((Source.Two << 2) & 10) | 1) ^ 3) > 0
                H = ((((Source.Two << 2) & 10) | 1) ^ 3) < 0
                I
            }
            """.trimIndent(),
            enumName = "Flags",
        )

        assertEquals(
            mapOf(
                "A" to "16",
                "B" to "8",
                "C" to "32",
                "D" to "0",
                "E" to "1",
                "F" to "2",
                "G" to "-1",
                "H" to "0",
                "I" to "1",
            ),
            values,
        )
    }

    fun `test enum values from division modulo and additional comparisons`() {
        val values = enumValues(
            """
            enum Math {
                Div = 17 / 5
                Mod = 17 % 5
                Eq = 10 == 10
                NotEqual = 10 != 9
                GreaterOrEqual = 10 >= 10
                LessOrEqual = 9 <= 8
                After
            }
            """.trimIndent(),
        )

        assertEquals(
            mapOf(
                "Div" to "3",
                "Mod" to "2",
                "Eq" to "-1",
                "NotEqual" to "-1",
                "GreaterOrEqual" to "-1",
                "LessOrEqual" to "0",
                "After" to "1",
            ),
            values,
        )
    }

    fun `test enum values stop after same enum reference initializer`() {
        val values = nullableEnumValues(
            """
            enum Codes {
                Base = 100
                Next = Codes.Base + 1
                After
            }
            """.trimIndent(),
        )

        assertEquals(
            mapOf(
                "Base" to "100",
                "Next" to null,
                "After" to null,
            ),
            values,
        )
    }

    fun `test circular enum references do not overflow stack`() {
        val firstValues = assertNoStackOverflow {
            nullableEnumValues(
                """
                enum First {
                    A = Second.B
                    B
                }

                enum Second {
                    B = First.A
                }
                """.trimIndent(),
                enumName = "First",
            )
        }
        val secondValues = assertNoStackOverflow {
            nullableEnumValues(
                """
                enum First {
                    A = Second.B
                }

                enum Second {
                    B = First.A
                    C
                }
                """.trimIndent(),
                enumName = "Second",
            )
        }

        assertEquals(
            mapOf(
                "A" to null,
                "B" to null,
            ),
            firstValues,
        )
        assertEquals(
            mapOf(
                "B" to null,
                "C" to null,
            ),
            secondValues,
        )
    }

    fun `test enum and constant circular references do not overflow stack`() {
        val values = assertNoStackOverflow {
            nullableEnumValues(
                """
                const RED = Color.Red;

                enum Color {
                    Red = RED
                    Green
                }
                """.trimIndent(),
            )
        }

        assertEquals(
            mapOf(
                "Red" to null,
                "Green" to null,
            ),
            values,
        )
    }

    fun `test enum values stop after unsupported initializer`() {
        val values = nullableEnumValues(
            """
            fun notConst(): int { return 10; }

            enum Broken {
                A = 1
                B = notConst()
                C
            }
            """.trimIndent(),
        )

        assertEquals(
            mapOf(
                "A" to "1",
                "B" to null,
                "C" to null,
            ),
            values,
        )
    }

    fun `test enum values stop after divide by zero initializer`() {
        val values = nullableEnumValues(
            """
            enum Broken {
                A = 1
                B = 10 / 0
                C
            }
            """.trimIndent(),
        )

        assertEquals(
            mapOf(
                "A" to "1",
                "B" to null,
                "C" to null,
            ),
            values,
        )
    }

    private fun <T> assertNoStackOverflow(block: () -> T): T = try {
        block()
    } catch (e: StackOverflowError) {
        throw AssertionError("Enum value evaluation must be guarded against circular references", e)
    }

    private fun enumValues(code: String, enumName: String? = null): Map<String, String> =
        nullableEnumValues(code, enumName).mapValues { (_, value) -> value!! }

    private fun nullableEnumValues(code: String, enumName: String? = null): Map<String, String?> {
        val file = myFixture.configureByText("test.tolk", code)
        val enum = PsiTreeUtil.findChildrenOfType(file, TolkEnum::class.java)
            .single { enumName == null || it.name == enumName }

        return enum.members.associate { member ->
            member.name!! to TolkEnumValueEvaluator.compute(member)?.toString()
        }
    }
}
