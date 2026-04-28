package org.ton.intellij.tolk.completion

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
}
