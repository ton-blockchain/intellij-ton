package org.ton.intellij.tolk.inspection.inspection

import org.ton.intellij.tolk.inspection.TolkInspectionTestBase
import org.ton.intellij.tolk.inspection.TolkTypeMismatchInspection

class TolkTypeMismatchInspectionTest : TolkInspectionTestBase() {
    fun `test no type mismatch in variable declaration with explicit hint`() {
        doInspectionTest(
            """
            fun main() {
                val a: int = 10;
            }
            """.trimIndent(),
            TolkTypeMismatchInspection()
        )
    }

    fun `test no type mismatch in variable declaration without explicit hint`() {
        doInspectionTest(
            """
            fun main() {
                val a = 10;
            }
            """.trimIndent(),
            TolkTypeMismatchInspection()
        )
    }

    fun `test type mismatch in variable declaration`() {
        doInspectionTest(
            """
            fun main() {
                val a: int = <error descr="Cannot assign 'slice' to variable of type 'int'">"hello"</error>;
            }
            """.trimIndent(),
            TolkTypeMismatchInspection()
        )
    }

    fun `test type mismatch in variable declaration with int8 ans int32 expression type`() {
        doInspectionTest(
            """
            fun main() {
                var a: int8 = <error descr="Cannot assign 'int32' to variable of type 'int8'">10 as int32</error>;
            }
            """.trimIndent(),
            TolkTypeMismatchInspection()
        )
    }

    fun `test no type mismatch in variable declaration with int8 ans int16 from match`() {
        doInspectionTest(
            """
            fun main(value: int) {
                var a: int = match (value) {
                    10 => 10 as int8,
                    else => 20 as int16,
                };
                __expect_type(a, "int");
            }
            """.trimIndent(),
            TolkTypeMismatchInspection()
        )
    }
}
