package org.ton.intellij.tolk.inspection

class TolkUnusedFunctionParameterInspectionTest : TolkInspectionTestBase() {
    fun `test ignore unused parameter with underscore prefix`() {
        checkNoProblems(
            """
            fun main(_value: int) {
            }
            """.trimIndent(),
            TolkUnusedFunctionParameterInspection(),
        )
    }

    fun `test ignore several unused parameters with underscore prefix`() {
        checkNoProblems(
            """
            fun main(_left: int, _right: int) {
            }
            """.trimIndent(),
            TolkUnusedFunctionParameterInspection(),
        )
    }
}
