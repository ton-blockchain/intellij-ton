package org.ton.intellij.func.inspection

class FuncUnresolvedReferenceInspectionTest : FuncInspectionTestBase() {
    fun `test var tensor destructuring underscore discard is not unresolved`() {
        checkNoProblems(
            """
            (int, int) f() asm "F";

            () main() impure {
                var (v110, _) = f();
            }
            """.trimIndent(),
            FuncUnresolvedReferenceInspection(),
        )
    }
}
