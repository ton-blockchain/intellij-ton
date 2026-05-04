package org.ton.intellij.acton.ide.newProject

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActonTemplateCatalogTest {
    @Test
    fun `fallback catalog matches bundled Acton templates`() {
        val catalog = ActonTemplateCatalogProvider.loadTemplateCatalog {
            ActonTemplateCatalogProvider.ActonTemplatesCommandOutput(
                exitCode = 1,
                stdout = "",
            )
        }

        assertEquals(
            listOf("empty", "counter", "jetton", "nft", "w5-extension"),
            catalog.templateIds(),
        )
        for (templateId in catalog.templateIds()) {
            assertTrue("$templateId should support TypeScript app scaffold", catalog.supportsTypeScriptApp(templateId))
        }

        assertEquals("contracts/Empty.tolk", catalog.starterFilePath("empty", includeTypeScriptApp = false))
        assertEquals("contracts/src/Empty.tolk", catalog.starterFilePath("empty", includeTypeScriptApp = true))
        assertEquals("contracts/Counter.tolk", catalog.starterFilePath("counter", includeTypeScriptApp = false))
        assertEquals("contracts/src/Counter.tolk", catalog.starterFilePath("counter", includeTypeScriptApp = true))
        assertEquals("contracts/JettonMinter.tolk", catalog.starterFilePath("jetton", includeTypeScriptApp = false))
        assertEquals("contracts/src/JettonMinter.tolk", catalog.starterFilePath("jetton", includeTypeScriptApp = true))
        assertEquals("contracts/NftCollection.tolk", catalog.starterFilePath("nft", includeTypeScriptApp = false))
        assertEquals("contracts/src/NftCollection.tolk", catalog.starterFilePath("nft", includeTypeScriptApp = true))
        assertEquals(
            "contracts/SimpleExtension.tolk",
            catalog.starterFilePath("w5-extension", includeTypeScriptApp = false),
        )
        assertEquals(
            "contracts/src/SimpleExtension.tolk",
            catalog.starterFilePath("w5-extension", includeTypeScriptApp = true),
        )
    }
}
