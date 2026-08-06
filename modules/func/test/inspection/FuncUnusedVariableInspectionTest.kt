package org.ton.intellij.func.inspection

class FuncUnusedVariableInspectionTest : FuncInspectionTestBase() {
    fun `test underscore-prefixed variables are intentionally unused`() {
        checkNoProblems(
            """
            (int, int) f() asm "F";

            () main() impure {
                int _local = 1;
                var (_value, _exists) = f();
            }
            """.trimIndent(),
            FuncUnusedVariableInspection(),
        )
    }

    fun `test underscore-prefixed function parameters are intentionally unused`() {
        checkNoProblems(
            """
            () main(int _flags) impure {
            }
            """.trimIndent(),
            FuncUnusedFunctionParameterInspection(),
        )
    }
}
