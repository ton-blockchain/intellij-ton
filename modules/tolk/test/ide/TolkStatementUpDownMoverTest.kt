package org.ton.intellij.tolk.ide

class TolkStatementUpDownMoverTest : TolkUpDownMoverTestBase() {
    fun `test move local variable statement down`() = moveDown(
        """
            fun main() {
                var /*caret*/first = 1;
                var second = 2;
            }
        """,
        """
            fun main() {
                var second = 2;
                var /*caret*/first = 1;
            }
        """,
    )

    fun `test move if statement up`() = moveUp(
        """
            fun main() {
                doFirst();
                if /*caret*/(true) {
                    doSecond();
                }
            }
        """,
        """
            fun main() {
                if /*caret*/(true) {
                    doSecond();
                }
                doFirst();
            }
        """,
    )
}
