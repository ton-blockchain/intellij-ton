package org.ton.intellij.acton.cli

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ActonTomlTest : BasePlatformTestCase() {
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
}
