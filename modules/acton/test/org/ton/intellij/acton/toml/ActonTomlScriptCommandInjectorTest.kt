package org.ton.intellij.acton.toml

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ActonTomlScriptCommandInjectorTest : BasePlatformTestCase() {
    fun testScriptsValueInjectsShellLanguage() {
        val injected = injectedElementAtCaret(
            """
                [scripts]
                deploy = "acton script scripts/deploy.tolk --net testnet<caret>"
            """.trimIndent()
        )

        assertShellLanguage(injected.containingFile.language.id, injected.containingFile.language.displayName)
    }

    fun testScriptsMultilineValueInjectsShellLanguage() {
        val injected = injectedElementAtCaret(
            """
                [scripts]
                verify = ${"\"\"\""}
                acton build &&
                acton test<caret>
                ${"\"\"\""}
            """.trimIndent()
        )

        assertShellLanguage(injected.containingFile.language.id, injected.containingFile.language.displayName)
    }

    fun testNonScriptsValueDoesNotInjectShellLanguage() {
        val file = myFixture.configureByText(
            "Acton.toml",
            """
                [package]
                description = "acton script scripts/deploy.tolk<caret>"
            """.trimIndent()
        )

        val injected = InjectedLanguageManager.getInstance(project).findInjectedElementAt(file, myFixture.caretOffset)
        assertNull(injected)
    }

    private fun injectedElementAtCaret(code: String): PsiElement = myFixture.configureByText("Acton.toml", code).let { file ->
        InjectedLanguageManager.getInstance(project).findInjectedElementAt(file, myFixture.caretOffset)
            ?: throw AssertionError("Expected injected language at caret")
    }

    private fun assertShellLanguage(languageId: String, displayName: String) {
        val matches = languageId in SHELL_LANGUAGE_IDS || displayName in SHELL_LANGUAGE_NAMES
        assertTrue("Expected shell language, got id=$languageId displayName=$displayName", matches)
    }

    private companion object {
        private val SHELL_LANGUAGE_IDS = setOf("Shell Script", "Bash", "sh")
        private val SHELL_LANGUAGE_NAMES = setOf("Shell Script", "Bash")
    }
}
