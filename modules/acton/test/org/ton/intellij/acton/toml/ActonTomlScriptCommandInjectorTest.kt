package org.ton.intellij.acton.toml

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ActonTomlScriptCommandInjectorTest : BasePlatformTestCase() {
    fun testNonScriptsValueDoesNotInjectShellLanguage() {
        val file = myFixture.configureByText(
            "Acton.toml",
            """
                [package]
                description = "acton script scripts/deploy.tolk<caret>"
            """.trimIndent(),
        )

        val injected = InjectedLanguageManager.getInstance(project).findInjectedElementAt(file, myFixture.caretOffset)
        assertNull(injected)
    }
}
