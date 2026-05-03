package org.ton.intellij.tolk.quickfix

import org.ton.intellij.tolk.inspection.TolkUnresolvedReferenceInspection

class TolkImportFileQuickFixTest : TolkQuickfixTestBase() {
    fun `test import file quickfix uses shortest acton mapping`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [import-mappings]
                tests = "tests"
                wrappers = "tests/wrappers"
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "tests/wrappers/wallet.tolk",
            """
                fun deployWallet() {}
            """.trimIndent(),
        )

        doQuickfixTest(
            """
            fun main() {
                deployWallet/*caret*/();
            }
            """.trimIndent(),
            """
            import "@wrappers/wallet"

            fun main() {
                deployWallet();
            }
            """.trimIndent(),
            "Import file",
            TolkUnresolvedReferenceInspection(),
        )
    }
}
