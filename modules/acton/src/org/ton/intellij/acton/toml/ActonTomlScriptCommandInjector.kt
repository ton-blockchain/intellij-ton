package org.ton.intellij.acton.toml

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlLiteral

class ActonTomlScriptCommandInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val literal = context as? TomlLiteral ?: return
        val valueContext = findActonTomlValueContext(literal) ?: return
        if (!valueContext.matches("scripts", null) || valueContext.isArrayItem) return

        val shellLanguage = SHELL_LANGUAGE_IDS.firstNotNullOfOrNull(Language::findLanguageByID)
            ?: return

        registrar.startInjecting(shellLanguage)
            .addPlace(null, null, literal, literal.valueTextRange())
            .doneInjecting()
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(TomlLiteral::class.java)
    }

    private companion object {
        private val SHELL_LANGUAGE_IDS = listOf("Shell Script", "Bash", "sh")
    }
}
