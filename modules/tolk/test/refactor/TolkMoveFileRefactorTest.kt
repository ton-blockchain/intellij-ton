package org.ton.intellij.tolk.refactor

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.psi.TolkFile

class TolkMoveFileRefactorTest : TolkTestBase() {
    fun `test bind to moved file keeps acton import mapping path`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [import-mappings]
                contracts = "contracts"
            """.trimIndent()
        )

        myFixture.addFileToProject("contracts/foo.tolk", "fun foo() {}")
        val movedFile = myFixture.addFileToProject("contracts/nested/foo.tolk", "fun foo() {}")
        val mainFile = myFixture.addFileToProject(
            "src/main.tolk",
            """
                import "@contracts/foo";

                fun main() {
                    foo();
                }
            """.trimIndent()
        )

        myFixture.configureFromExistingVirtualFile(mainFile.virtualFile)
        val file = myFixture.file as TolkFile
        val include = file.includeDefinitions.single()
        val reference = include.stringLiteral?.references?.last() as? FileReference
        requireNotNull(reference) { "Expected file reference in include path" }

        WriteCommandAction.runWriteCommandAction(project) {
            reference.bindToElement(movedFile)
        }

        myFixture.checkResult(
            """
                import "@contracts/nested/foo";

                fun main() {
                    foo();
                }
            """.trimIndent()
        )
    }
}
