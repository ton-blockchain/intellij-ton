package org.ton.intellij.acton.toml

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDirectory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlLiteral

class ActonTomlReferenceContributorTest : BasePlatformTestCase() {
    fun testImportMappingsTableProvidesFileReferences() {
        myFixture.addFileToProject("contracts/example.tolk", "fun main() {}")

        val file = myFixture.configureByText(
            "Acton.toml",
            """
                [import-mappings]
                contracts = "cont<caret>racts"
            """.trimIndent()
        )

        val literal = PsiTreeUtil.getParentOfType(file.findElementAt(myFixture.caretOffset), TomlLiteral::class.java, false)
        assertNotNull(literal)

        val references = ActonTomlValueReferenceProvider().getReferencesByElement(literal!!, ProcessingContext())
        assertTrue("Expected file references for import-mappings entry", references.isNotEmpty())
        val resolved = references.last().resolve()
        val resolvedName = (resolved as? PsiDirectory)?.virtualFile?.name
            ?: (resolved as? PsiElement)?.containingFile?.virtualFile?.name
            ?: resolved?.text
        assertEquals("contracts", resolvedName)
    }
}
