package org.ton.intellij.tolk.resolving

class TolkResolveStdlibTest : TolkResolvingTestBase() {
    fun `test nested acton stdlib common is imported by default`() {
        projectFile(
            "packages/app/Acton.toml",
            "",
        )
        projectFile(
            "packages/app/.acton/tolk-stdlib/common.tolk",
            """
            type FromAppStdlib = int;
            """.trimIndent(),
        )
        projectFile(
            "packages/wallet/Acton.toml",
            "",
        )
        projectFile(
            "packages/wallet/.acton/tolk-stdlib/common.tolk",
            """
            type FromWalletStdlib = int;
            """.trimIndent(),
        )
        mainProjectFile(
            "packages/app/contracts/main.tolk",
            """
            fun main(value: /*caret*/FromAppStdlib) {}
            """.trimIndent(),
        )

        assertReferencedTo("TYPE_DEF:FromAppStdlib FromAppStdlib")
    }
}
