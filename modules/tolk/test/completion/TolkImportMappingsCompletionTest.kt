package org.ton.intellij.tolk.completion

import org.ton.intellij.util.presentation

class TolkImportMappingsCompletionTest : TolkCompletionTestBase() {
    fun `test acton import mapping completion in empty import`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [import-mappings]
                contracts = "contracts"
            """.trimIndent(),
        )

        checkContainsCompletion(
            "@contracts",
            """
            import "/*caret*/"
            """.trimIndent(),
        )
    }

    fun `test unimported symbol completion uses shortest acton mapping`() {
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

        checkContainsCompletion(
            "deployWallet() (@wrappers/wallet)",
            """
            fun main() {
                deploy/*caret*/
            }
            """.trimIndent(),
            render = { lookupString + presentation.tailText },
        )
    }

    fun `test unimported symbol completion inserts shortest acton mapping import`() {
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

        checkCompletion(
            "deployWallet",
            """
            fun main() {
                deploy/*caret*/
            }
            """.trimIndent(),
            """
            import "@wrappers/wallet"

            fun main() {
                deployWallet()/*caret*/
            }
            """.trimIndent(),
        )
    }
}
