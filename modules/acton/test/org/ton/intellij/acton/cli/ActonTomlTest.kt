package org.ton.intellij.acton.cli

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.ton.intellij.acton.ide.hasActonToml
import org.ton.intellij.acton.ide.setTomlPluginInstalledOverride

class ActonTomlTest : BasePlatformTestCase() {
    override fun tearDown() {
        setTomlPluginInstalledOverride(null)
        super.tearDown()
    }

    fun testFindProjectUsesProjectRootActonToml() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.root]
                src = "contracts/root.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "packages/app/Acton.toml",
            """
                [contracts.app]
                src = "contracts/app.tolk"
            """.trimIndent(),
        )

        val actonToml = ActonToml.find(project)

        assertNotNull(actonToml)
        assertTrue(actonToml!!.virtualFile.path.replace('\\', '/').endsWith("/Acton.toml"))
        assertEquals(listOf("root"), actonToml.getContractIds())
    }

    fun testFindFromFileUsesNearestActonToml() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.root]
                src = "contracts/root.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "packages/app/Acton.toml",
            """
                [contracts.app]
                src = "contracts/app.tolk"
            """.trimIndent(),
        )
        val sourceFile = myFixture.addFileToProject("packages/app/contracts/app.tolk", "fun main() {}")

        val actonToml = ActonToml.find(project, sourceFile.virtualFile)

        assertNotNull(actonToml)
        assertTrue(actonToml!!.virtualFile.path.replace('\\', '/').endsWith("/packages/app/Acton.toml"))
        assertEquals(listOf("app"), actonToml.getContractIds())
    }

    fun testFindProjectDoesNotPickNestedActonToml() {
        myFixture.addFileToProject(
            "packages/app/Acton.toml",
            """
                [contracts.app]
                src = "contracts/app.tolk"
            """.trimIndent(),
        )

        assertNull(ActonToml.find(project))
    }

    fun testFindReturnsNullWhenTomlPluginIsUnavailable() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
            [contracts.counter]
            src = "contracts/counter.tolk"
            """.trimIndent(),
        )

        setTomlPluginInstalledOverride(false)

        assertNull(ActonToml.find(project))
    }

    fun testHasActonTomlChecksOnlyProjectRoot() {
        myFixture.addFileToProject(
            "packages/app/Acton.toml",
            """
                [contracts.app]
                src = "contracts/app.tolk"
            """.trimIndent(),
        )

        assertFalse(hasActonToml(project))
    }

    fun testLocalnetSettingsAreParsedFromActonToml() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [localnet]
                port = 3010
                fork-net = "testnet"
                fork-block-number = 55000000
                accounts = ["deployer", "user"]
                rate-limit = 2
            """.trimIndent(),
        )

        val localnet = ActonToml.find(project)?.getLocalnetSettings()

        assertNotNull(localnet)
        assertEquals(3010, localnet!!.port)
        assertEquals("testnet", localnet.forkNet)
        assertEquals(55_000_000L, localnet.forkBlockNumber)
        assertEquals(listOf("deployer", "user"), localnet.accounts)
        assertEquals(2, localnet.rateLimit)
    }
}
