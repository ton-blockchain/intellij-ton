package org.ton.intellij.acton.toml

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDirectory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlKeySegment

class ActonTomlReferenceContributorTest : BasePlatformTestCase() {
    fun testImportMappingsTableProvidesFileReferences() {
        myFixture.addFileToProject("contracts/example.tolk", "fun main() {}")

        val resolved = resolveLiteralReference(
            """
                [import-mappings]
                contracts = "cont<caret>racts"
            """.trimIndent()
        )
        val resolvedName = (resolved as? PsiDirectory)?.virtualFile?.name
            ?: (resolved as? PsiElement)?.containingFile?.virtualFile?.name
            ?: resolved?.text
        assertEquals("contracts", resolvedName)
    }

    fun testDependsArrayItemResolvesToContractDefinition() {
        val resolved = resolveLiteralReference(
            """
                [contracts.counter]
                src = "contracts/counter.tolk"

                [contracts.app]
                src = "contracts/app.tolk"
                depends = ["cou<caret>nter"]
            """.trimIndent()
        )

        assertEquals("counter", (resolved as? TomlKeySegment)?.name)
        assertEquals("Acton.toml", resolved.containingFile.name)
    }

    fun testDependsInlineNameResolvesToContractDefinition() {
        val resolved = resolveLiteralReference(
            """
                [contracts.counter]
                src = "contracts/counter.tolk"

                [contracts.app]
                src = "contracts/app.tolk"
                depends = [{ name = "cou<caret>nter" }]
            """.trimIndent()
        )

        assertEquals("counter", (resolved as? TomlKeySegment)?.name)
        assertEquals("Acton.toml", resolved.containingFile.name)
    }

    fun testLitenodeAccountResolvesToWalletDefinition() {
        myFixture.addFileToProject(
            "wallets.toml",
            """
                [wallets.alice]
                mnemonic = "test test test"
            """.trimIndent()
        )

        val resolved = resolveLiteralReference(
            """
                [litenode]
                accounts = ["ali<caret>ce"]
            """.trimIndent()
        )

        assertEquals("alice", (resolved as? TomlKeySegment)?.name)
        assertEquals("wallets.toml", resolved.containingFile.name)
    }

    fun testForkNetCustomResolvesToNetworkDefinition() {
        val resolved = resolveLiteralReference(
            """
                [networks.devnet]

                [test]
                fork-net = { custom = "dev<caret>net" }
            """.trimIndent()
        )

        assertEquals("devnet", (resolved as? TomlKeySegment)?.name)
        assertEquals("Acton.toml", resolved.containingFile.name)
    }

    fun testLintRuleContractOverrideKeyResolvesToContractDefinition() {
        val resolved = resolveKeyReference(
            """
                [contracts.counter]
                src = "contracts/counter.tolk"

                [lint.rules.no-inline]
                cou<caret>nter = "warn"
            """.trimIndent()
        )

        assertEquals("counter", (resolved as? TomlKeySegment)?.name)
        assertEquals("Acton.toml", resolved.containingFile.name)
    }

    private fun resolveLiteralReference(code: String): PsiElement {
        val file = myFixture.configureByText("Acton.toml", code)
        val literal = PsiTreeUtil.getParentOfType(file.findElementAt(myFixture.caretOffset), TomlLiteral::class.java, false)
        assertNotNull(literal)

        val references = literal!!.references
        assertTrue("Expected references for `${literal.text}`", references.isNotEmpty())
        for (reference in references.indices.reversed().map { references[it] }) {
            reference.resolve()?.let { return it }
        }
        throw AssertionError("Expected a resolvable reference for `${literal.text}`")
    }

    private fun resolveKeyReference(code: String): PsiElement {
        val file = myFixture.configureByText("Acton.toml", code)
        val keySegment = PsiTreeUtil.getParentOfType(file.findElementAt(myFixture.caretOffset), TomlKeySegment::class.java, false)
        assertNotNull(keySegment)

        val references = keySegment!!.references
        assertTrue("Expected references for `${keySegment.text}`", references.isNotEmpty())
        for (reference in references.indices.reversed().map { references[it] }) {
            reference.resolve()?.let { return it }
        }
        throw AssertionError("Expected a resolvable reference for `${keySegment.text}`")
    }
}
