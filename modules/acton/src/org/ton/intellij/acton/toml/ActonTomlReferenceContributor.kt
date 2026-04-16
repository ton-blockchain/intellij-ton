package org.ton.intellij.acton.toml

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTable
import org.ton.intellij.acton.cli.ActonToml

class ActonTomlReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            psiElement(TomlLiteral::class.java),
            ActonTomlValueReferenceProvider()
        )
        registrar.registerReferenceProvider(
            psiElement(TomlKeySegment::class.java),
            ActonTomlKeyReferenceProvider()
        )
    }
}

class ActonTomlValueReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val valueContext = findActonTomlValueContext(element) ?: return PsiReference.EMPTY_ARRAY
        val references = mutableListOf<PsiReference>()
        if (valueContext.isPathField()) {
            references += createFileReferences(valueContext.literal)
        }

        val actonToml = element.containingActonToml()
        val target = when {
            valueContext.matches("litenode", "accounts") && valueContext.isArrayItem -> {
                actonToml?.getWallets()
                    ?.find { it.name == valueContext.literal.stringValue() }
                    ?.element
            }

            (valueContext.matches("contracts", null, "depends") && valueContext.isArrayItem)
                || valueContext.matches("contracts", null, "depends", "name") -> {
                actonToml?.getContractElements()
                    ?.find { it.name == valueContext.literal.stringValue() }
            }

            valueContext.matches("test", "fork-net", "custom")
                || valueContext.matches("litenode", "fork-net", "custom") -> {
                actonToml?.getCustomNetworkElements()
                    ?.find { it.name == valueContext.literal.stringValue() }
            }

            else -> null
        }
        if (target != null) {
            references += ActonTomlLiteralReference(valueContext.literal, target)
        }

        return references.toTypedArray()
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

class ActonTomlKeyReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val keySegment = element as? TomlKeySegment ?: return PsiReference.EMPTY_ARRAY
        if (keySegment.containingFile.name != "Acton.toml") return PsiReference.EMPTY_ARRAY

        val keyValue = keySegment.parent?.parent as? TomlKeyValue ?: return PsiReference.EMPTY_ARRAY
        if (keyValue.key.segments.singleOrNull() != keySegment) return PsiReference.EMPTY_ARRAY

        val table = PsiTreeUtil.getParentOfType(keyValue, TomlTable::class.java, false) ?: return PsiReference.EMPTY_ARRAY
        val headerSegments = table.header.key?.segments ?: return PsiReference.EMPTY_ARRAY
        if (headerSegments.size != 3 || headerSegments[0].name != "lint" || headerSegments[1].name != "rules") {
            return PsiReference.EMPTY_ARRAY
        }

        val target = element.containingActonToml()
            ?.getContractElements()
            ?.find { it.name == keySegment.name }
            ?: return PsiReference.EMPTY_ARRAY

        return arrayOf(ActonTomlKeySegmentReference(keySegment, target))
    }
}

private class ActonTomlLiteralReference(
    element: TomlLiteral,
    private val target: PsiElement,
) : PsiReferenceBase<TomlLiteral>(element, element.valueTextRange(), false) {
    override fun resolve(): PsiElement = target
}

private class ActonTomlKeySegmentReference(
    element: TomlKeySegment,
    private val target: PsiElement,
) : PsiReferenceBase<TomlKeySegment>(element, TextRange(0, element.textLength), false) {
    override fun resolve(): PsiElement = target
}

private fun PsiElement.containingActonToml(): ActonToml? {
    val file = containingFile?.virtualFile ?: return null
    if (file.name != "Acton.toml") return null
    return ActonToml(file, project)
}
