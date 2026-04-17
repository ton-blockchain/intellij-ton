package org.ton.intellij.tolk.ide

class TolkItemUpDownMoverTest : TolkUpDownMoverTestBase() {
    fun `test move top level declaration down`() = moveDown(
        """
            fun /*caret*/first() {}

            fun second() {}
        """,
        """
            fun second() {}

            fun /*caret*/first() {}
        """,
    )

    fun `test move declaration with doc comment up`() = moveUp(
        """
            fun first() {}

            /// docs for second
            fun /*caret*/second() {}
        """,
        """
            /// docs for second
            fun /*caret*/second() {}

            fun first() {}
        """,
    )
}
