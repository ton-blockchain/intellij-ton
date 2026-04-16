package org.ton.intellij.acton.toml

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlLiteral

class ActonTomlReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            psiElement(TomlLiteral::class.java),
            ActonTomlValueReferenceProvider()
        )
    }
}

class ActonTomlValueReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val valueContext = findActonTomlValueContext(element) ?: return PsiReference.EMPTY_ARRAY
        if (valueContext.isPathField()) {
            return createFileReferences(valueContext.literal)
        }

        return PsiReference.EMPTY_ARRAY
    }

    private fun createFileReferences(literal: TomlLiteral): Array<PsiReference> {
        val text = literal.text
        if (text.length >= 2 && ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'")))) {
            val path = text.substring(1, text.length - 1)
            return FileReferenceSet(path, literal, 1, null, true).allReferences
                .map { it as PsiReference }
                .toTypedArray()
        }
        return PsiReference.EMPTY_ARRAY
    }

    private fun ActonTomlValueContext.isPathField(): Boolean {
        return matches("build", "gen-dir")
            || matches("build", "out-dir")
            || matches("build", "output-fift")
            || matches("import-mappings", null)
            || matches("contracts", null, "src")
            || matches("contracts", null, "output")
            || matches("contracts", null, "depends", "path")
            || matches("test", "coverage", "output-file")
            || matches("test", "junit-path")
            || matches("test", "mutation", "rules-file")
            || matches("wrappers", "tolk", "output-dir")
            || matches("wrappers", "tolk", "test-output-dir")
            || matches("wrappers", "typescript", "output-dir")
    }
}
