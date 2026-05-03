package org.ton.intellij.acton.toml

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ActonInitializeDappActionTest : BasePlatformTestCase() {
    fun testCanInitializeDappForActonTomlWithoutAppDirectory() {
        val actonToml = myFixture.addFileToProject("Acton.toml", "")

        assertTrue(canInitializeDapp(actonToml.virtualFile))
    }

    fun testCannotInitializeDappForActonTomlWithAppDirectory() {
        val actonToml = myFixture.addFileToProject("Acton.toml", "")
        myFixture.addFileToProject("app/package.json", "{}")

        assertFalse(canInitializeDapp(actonToml.virtualFile))
    }

    fun testCannotInitializeDappForNonActonTomlFile() {
        val file = myFixture.addFileToProject("Other.toml", "")

        assertFalse(canInitializeDapp(file.virtualFile))
    }
}
