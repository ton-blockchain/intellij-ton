package org.ton.intellij.acton.toml

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable

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
        val literal = element as? TomlLiteral ?: return PsiReference.EMPTY_ARRAY
        if (literal.containingFile.name != "Acton.toml") return PsiReference.EMPTY_ARRAY

        val keyValue = literal.parent as? TomlKeyValue ?: return PsiReference.EMPTY_ARRAY
        val key = keyValue.key.text
        val table = keyValue.parent as? TomlTable ?: return PsiReference.EMPTY_ARRAY
        val header = table.header
        val segments = header.key?.segments ?: return PsiReference.EMPTY_ARRAY

        if (key == "src" || key == "output") {
            if (segments.size == 2 && segments[0].name == "contracts") {
                return createFileReferences(literal)
            }
        }

        if (segments.size == 1 && segments[0].name == "mappings") {
            return createFileReferences(literal)
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
}
