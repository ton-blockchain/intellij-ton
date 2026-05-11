package org.ton.intellij.tolk.resolving

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.toml.lang.psi.TomlKeySegment
import org.ton.intellij.tolk.psi.TolkContractDefinition
import org.ton.intellij.tolk.replaceCaretMarker

class TolkContractDefinitionReferenceTest : BasePlatformTestCase() {
    fun `test contract header name resolves to acton contract entry`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.Counter]
                src = "contracts/Counter.tolk"
            """.trimIndent(),
        )

        val resolved = resolveContractHeaderReference(
            "contracts/Counter.tolk",
            """
            contract Cou/*caret*/nter {}
            """.trimIndent(),
        )

        assertEquals("Counter", (resolved as? TomlKeySegment)?.name)
        assertEquals("Acton.toml", resolved.containingFile.name)
    }

    fun `test contract header name uses nearest acton toml`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.Counter]
                src = "contracts/Counter.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "packages/app/Acton.toml",
            """
                [contracts.Counter]
                src = "contracts/src/Counter.tolk"
            """.trimIndent(),
        )

        val resolved = resolveContractHeaderReference(
            "packages/app/contracts/src/Counter.tolk",
            """
            contract Cou/*caret*/nter {}
            """.trimIndent(),
        )

        assertEquals("Counter", (resolved as? TomlKeySegment)?.name)
        assertTrue(resolved.containingFile.virtualFile.path.replace('\\', '/').endsWith("/packages/app/Acton.toml"))
    }

    fun `test unregistered contract header name has no reference`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.Counter]
                src = "contracts/Counter.tolk"
            """.trimIndent(),
        )

        val reference = findContractHeaderReference(
            "contracts/Wallet.tolk",
            """
            contract Wal/*caret*/let {}
            """.trimIndent(),
        )

        assertNull(reference)
    }

    fun `test acton contract entry finds contract header usage`() {
        val actonToml = myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.Counter]
                src = "contracts/Counter.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "contracts/Counter.tolk",
            """
            contract Counter {}
            """.trimIndent(),
        )
        val contractEntry = PsiTreeUtil.findChildrenOfType(actonToml, TomlKeySegment::class.java)
            .single { it.name == "Counter" }

        val references = ReferencesSearch.search(contractEntry).findAll()

        assertTrue(references.any { it.element.containingFile.name == "Counter.tolk" })
    }

    fun `test contract header target presentation is available`() {
        val file = myFixture.addFileToProject(
            "contracts/Counter.tolk",
            """
            contract Counter {}
            """.trimIndent(),
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        val contract = PsiTreeUtil.findChildOfType(myFixture.file, TolkContractDefinition::class.java)

        assertEquals("Counter", contract?.presentation?.presentableText)
    }

    private fun resolveContractHeaderReference(path: String, code: String): PsiElement {
        val reference = findContractHeaderReference(path, code)
        assertNotNull("Expected reference in $path", reference)
        return reference!!.resolve() ?: throw AssertionError("Expected resolved reference in $path")
    }

    private fun findContractHeaderReference(path: String, code: String): PsiReference? {
        val file = myFixture.addFileToProject(path, replaceCaretMarker(code))
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        return myFixture.file.findReferenceAt(myFixture.caretOffset)
    }
}
