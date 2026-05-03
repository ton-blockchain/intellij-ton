package org.ton.intellij.tolk.psi

import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.ide.configurable.tolkSettings

class TolkImportPathTest : TolkTestBase() {
    fun `test compute shortest acton import mapping path`() {
        addActonToml(
            """
            [import-mappings]
            tests = "tests"
            wrappers = "tests/wrappers"
            """.trimIndent(),
        )
        val sourceFile = addTolkFile("src/main.tolk")
        val targetFile = addTolkFile("tests/wrappers/wallet.tolk")

        assertEquals(
            "@wrappers/wallet",
            computeTolkImportPath(project, sourceFile.virtualFile, targetFile.virtualFile),
        )
    }

    fun `test acton mapping path respects directory boundary`() {
        addActonToml(
            """
            [import-mappings]
            tests = "tests"
            """.trimIndent(),
        )
        val sourceFile = addTolkFile("src/main.tolk")
        val targetFile = addTolkFile("tests-extra/wallet.tolk")

        assertEquals(
            "../tests-extra/wallet",
            computeTolkImportPath(project, sourceFile.virtualFile, targetFile.virtualFile),
        )
    }

    fun `test compute relative import path without acton mapping`() {
        val sourceFile = addTolkFile("src/main.tolk")
        val targetFile = addTolkFile("lib/wallet.tolk")

        assertEquals(
            "../lib/wallet",
            computeTolkImportPath(project, sourceFile.virtualFile, targetFile.virtualFile),
        )
    }

    fun `test compute stdlib import path`() {
        val sourceFile = addTolkFile("src/main.tolk")
        val stdlibFile = addTolkFile("tolk-stdlib/gas-payments.tolk")
        project.tolkSettings.stdlibPath = stdlibFile.virtualFile.parent.url

        assertEquals(
            "@stdlib/gas-payments",
            computeTolkImportPath(project, sourceFile.virtualFile, stdlibFile.virtualFile),
        )
    }

    fun `test acton import mapping paths are sorted by length then name`() {
        addActonToml(
            """
            [import-mappings]
            b = "tests/wrappers"
            a = "tests/wrappers"
            long = "tests"
            """.trimIndent(),
        )
        val sourceFile = addTolkFile("src/main.tolk")
        val targetFile = addTolkFile("tests/wrappers/wallet.tolk")
        val actonToml = org.ton.intellij.acton.cli.ActonToml.find(project, sourceFile.virtualFile)

        assertEquals(
            listOf("@a/wallet", "@b/wallet", "@long/wrappers/wallet"),
            actonImportMappingPaths(targetFile.virtualFile, actonToml),
        )
        assertEquals(
            "@a/wallet",
            shortestActonImportMappingPath(targetFile.virtualFile, actonToml),
        )
    }

    private fun addActonToml(text: String) {
        myFixture.addFileToProject("Acton.toml", text)
    }

    private fun addTolkFile(path: String): TolkFile =
        myFixture.addFileToProject(path, "fun placeholder() {}") as TolkFile
}
