package org.ton.intellij.tolk.resolving

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.toml.lang.psi.TomlKeySegment
import org.ton.intellij.tolk.psi.TolkStringLiteral

class TolkActonStringArgumentReferenceTest : BasePlatformTestCase() {
    fun `test scripts wallet string resolves to wallet definition`() {
        myFixture.addFileToProject("Acton.toml", "")
        myFixture.addFileToProject(
            "wallets.toml",
            """
                [wallets.alice]
                mnemonic = "local"
            """.trimIndent(),
        )

        val resolved = resolveStringReference(
            """
                fun main() {
                    scripts.wallet("ali<caret>ce")
                }
            """.trimIndent(),
        )

        assertEquals("alice", (resolved as? TomlKeySegment)?.name)
        assertEquals("wallets.toml", resolved.containingFile.name)
    }

    fun `test net wallet string does not resolve`() {
        myFixture.addFileToProject("Acton.toml", "")
        myFixture.addFileToProject(
            "wallets.toml",
            """
                [wallets.alice]
                mnemonic = "local"
            """.trimIndent(),
        )

        val file = myFixture.configureByText(
            "scripts/deploy.tolk",
            """
                fun main() {
                    net.wallet("ali<caret>ce")
                }
            """.trimIndent(),
        )
        val stringLiteral = PsiTreeUtil.getParentOfType(
            file.findElementAt(myFixture.caretOffset),
            TolkStringLiteral::class.java,
            false,
        )
        assertNotNull(stringLiteral)

        val references = stringLiteral!!.references
        assertTrue("Expected unresolved reference for `${stringLiteral.text}`", references.all { it.resolve() == null })
    }

    private fun resolveStringReference(code: String): PsiElement {
        val file = myFixture.configureByText("scripts/deploy.tolk", code)
        val stringLiteral = PsiTreeUtil.getParentOfType(
            file.findElementAt(myFixture.caretOffset),
            TolkStringLiteral::class.java,
            false,
        )
        assertNotNull(stringLiteral)

        val references = stringLiteral!!.references
        assertTrue("Expected references for `${stringLiteral.text}`", references.isNotEmpty())
        for (reference in references.indices.reversed().map(references::get)) {
            reference.resolve()?.let { return it }
        }
        throw AssertionError("Expected a resolvable reference for `${stringLiteral.text}`")
    }
}
