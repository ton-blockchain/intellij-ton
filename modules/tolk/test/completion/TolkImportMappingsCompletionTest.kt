package org.ton.intellij.tolk.completion

import com.intellij.codeInsight.CodeInsightSettings
import junit.framework.TestCase.assertNotNull
import org.ton.intellij.tolk.replaceCaretMarker
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

    fun `test acton mapped helpers are hidden in app contract sources`() {
        setupActonAppScaffoldCompletionFiles()

        val variants = completeBasicInFile(
            "contracts/src/Counter.tolk",
            """
            fun main() {
                acton/*caret*/
            }
            """.trimIndent(),
        )

        assertFalse(variants.contains("actonOnlyHelper"))
    }

    fun `test acton mapped helpers are visible in app contract scripts`() {
        setupActonAppScaffoldCompletionFiles()

        val variants = completeBasicInFile(
            "contracts/scripts/deploy.tolk",
            """
            fun main() {
                acton/*caret*/
            }
            """.trimIndent(),
        )

        assertTrue(variants.contains("actonOnlyHelper"))
    }

    fun `test acton mapped helpers are hidden in standard contract sources`() {
        setupActonScaffoldCompletionFiles(contractsMapping = "contracts")

        val variants = completeBasicInFile(
            "contracts/Counter.tolk",
            """
            fun main() {
                acton/*caret*/
            }
            """.trimIndent(),
        )

        assertFalse(variants.contains("actonOnlyHelper"))
    }

    fun `test acton stdlib symbols remain visible in app contract sources`() {
        setupActonAppScaffoldCompletionFiles()
        myFixture.addFileToProject(
            ".acton/tolk-stdlib/common.tolk",
            """
                fun stdlibVisibleHelper() {}
            """.trimIndent(),
        )

        val variants = completeBasicInFile(
            "contracts/src/Counter.tolk",
            """
            fun main() {
                stdlib/*caret*/
            }
            """.trimIndent(),
        )

        assertTrue(variants.contains("stdlibVisibleHelper"))
    }

    fun `test app contract source mapping respects directory boundary`() {
        setupActonAppScaffoldCompletionFiles()

        val variants = completeBasicInFile(
            "contracts/src_extra/Tool.tolk",
            """
            fun main() {
                acton/*caret*/
            }
            """.trimIndent(),
        )

        assertTrue(variants.contains("actonOnlyHelper"))
    }

    fun `test nested acton app scripts use nearest contract source mapping`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [import-mappings]
                contracts = "contracts"
            """.trimIndent(),
        )
        setupActonAppScaffoldCompletionFiles(root = "packages/app")

        val variants = completeBasicInFile(
            "packages/app/contracts/scripts/deploy.tolk",
            """
            fun main() {
                acton/*caret*/
            }
            """.trimIndent(),
        )

        assertTrue(variants.contains("actonOnlyHelper"))
    }

    private fun setupActonAppScaffoldCompletionFiles() {
        setupActonScaffoldCompletionFiles(contractsMapping = "contracts/src")
    }

    private fun setupActonAppScaffoldCompletionFiles(root: String) {
        setupActonScaffoldCompletionFiles(contractsMapping = "contracts/src", root = root)
    }

    private fun setupActonScaffoldCompletionFiles(contractsMapping: String, root: String = "") {
        val prefix = root.takeIf { it.isNotEmpty() }?.let { "$it/" }.orEmpty()
        myFixture.addFileToProject(
            "${prefix}Acton.toml",
            """
                [import-mappings]
                acton = ".acton"
                contracts = "$contractsMapping"
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "$prefix.acton/io.tolk",
            """
                fun actonOnlyHelper() {}
            """.trimIndent(),
        )
    }

    private fun completeBasicInFile(path: String, code: String): List<String> {
        val file = myFixture.addFileToProject(path, replaceCaretMarker(code))
        myFixture.configureFromExistingVirtualFile(file.virtualFile)

        val oldAutocomplete = CodeInsightSettings.getInstance().AUTOCOMPLETE_ON_CODE_COMPLETION
        CodeInsightSettings.getInstance().AUTOCOMPLETE_ON_CODE_COMPLETION = false
        try {
            val lookups = myFixture.completeBasic()
            assertNotNull("Expected completions in $path", lookups)
            return lookups!!.map { it.lookupString }
        } finally {
            CodeInsightSettings.getInstance().AUTOCOMPLETE_ON_CODE_COMPLETION = oldAutocomplete
        }
    }
}
