package org.ton.intellij.tolk.ide.configurable

import org.ton.intellij.tolk.TolkTestBase

class TolkProjectSettingsServiceTest : TolkTestBase() {
    fun `test nested acton stdlib is detected from context file`() {
        myFixture.addFileToProject("packages/app/Acton.toml", "")
        myFixture.addFileToProject("packages/app/.acton/tolk-stdlib/common.tolk", "fun appStdlib() {}")
        myFixture.addFileToProject("packages/wallet/Acton.toml", "")
        myFixture.addFileToProject("packages/wallet/.acton/tolk-stdlib/common.tolk", "fun walletStdlib() {}")
        val sourceFile = myFixture.addFileToProject("packages/app/contracts/main.tolk", "fun main() {}")

        val stdlibDir = project.tolkSettings.stdlibDirFor(sourceFile.virtualFile)

        assertNotNull(stdlibDir)
        assertTrue(stdlibDir!!.path.replace('\\', '/').endsWith("/packages/app/.acton/tolk-stdlib"))
        assertTrue(project.tolkSettings.hasStdlibFor(sourceFile.virtualFile))
    }

    fun `test project stdlib remains root based without context file`() {
        myFixture.addFileToProject("packages/app/Acton.toml", "")
        myFixture.addFileToProject("packages/app/.acton/tolk-stdlib/common.tolk", "fun appStdlib() {}")

        assertNull(project.tolkSettings.stdlibDir)
        assertFalse(project.tolkSettings.hasStdlib)
    }

    fun `test root auto detected stdlib does not override nested acton context`() {
        myFixture.addFileToProject(".acton/tolk-stdlib/common.tolk", "fun rootStdlib() {}")
        myFixture.addFileToProject("packages/app/Acton.toml", "")
        myFixture.addFileToProject("packages/app/.acton/tolk-stdlib/common.tolk", "fun appStdlib() {}")
        val sourceFile = myFixture.addFileToProject("packages/app/contracts/main.tolk", "fun main() {}")

        val projectStdlibDir = project.tolkSettings.stdlibDir
        val contextStdlibDir = project.tolkSettings.stdlibDirFor(sourceFile.virtualFile)

        assertNotNull(projectStdlibDir)
        assertNotNull(contextStdlibDir)
        assertTrue(projectStdlibDir!!.path.replace('\\', '/').endsWith("/.acton/tolk-stdlib"))
        assertTrue(contextStdlibDir!!.path.replace('\\', '/').endsWith("/packages/app/.acton/tolk-stdlib"))
    }

    fun `test root stdlib is not used when nested acton stdlib is missing`() {
        myFixture.addFileToProject(".acton/tolk-stdlib/common.tolk", "fun rootStdlib() {}")
        myFixture.addFileToProject("packages/app/Acton.toml", "")
        val sourceFile = myFixture.addFileToProject("packages/app/contracts/main.tolk", "fun main() {}")

        assertNotNull(project.tolkSettings.stdlibDir)
        assertNull(project.tolkSettings.stdlibDirFor(sourceFile.virtualFile))
        assertFalse(project.tolkSettings.hasStdlibFor(sourceFile.virtualFile))
    }

    fun `test nested stdlib cache updates after stdlib is created`() {
        myFixture.addFileToProject("packages/app/Acton.toml", "")
        val sourceFile = myFixture.addFileToProject("packages/app/contracts/main.tolk", "fun main() {}")

        assertNull(project.tolkSettings.stdlibDirFor(sourceFile.virtualFile))

        myFixture.addFileToProject("packages/app/.acton/tolk-stdlib/common.tolk", "fun appStdlib() {}")

        val stdlibDir = project.tolkSettings.stdlibDirFor(sourceFile.virtualFile)
        assertNotNull(stdlibDir)
        assertTrue(stdlibDir!!.path.replace('\\', '/').endsWith("/packages/app/.acton/tolk-stdlib"))
    }
}
