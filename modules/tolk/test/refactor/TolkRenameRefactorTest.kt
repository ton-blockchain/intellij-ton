package org.ton.intellij.tolk.refactor

import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenameProcessor
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlKeySegment
import org.ton.intellij.tolk.psi.TolkContractDefinition
import org.ton.intellij.tolk.psi.TolkFile

class TolkRenameRefactorTest : TolkRefactorTestBase() {
    fun `test renaming contract updates source file and Acton configuration`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.Foo]
                src = "contracts/src/Foo.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "contracts/src/Foo.tolk",
            """
                contract Foo {}
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "scripts/build.tolk",
            """
                fun buildContract() {
                    build("Foo");
                }
            """.trimIndent(),
        )

        val projectDir = project.guessProjectDir()!!
        val sourceVirtualFile = projectDir.findFileByRelativePath("contracts/src/Foo.tolk")!!
        myFixture.configureFromExistingVirtualFile(sourceVirtualFile)
        val contract = PsiTreeUtil.findChildOfType(myFixture.file, TolkContractDefinition::class.java)!!

        RenameProcessor(project, contract, "Bar", false, false).run()

        val renamedVirtualFile = projectDir.findFileByRelativePath("contracts/src/Bar.tolk")
        assertNotNull(renamedVirtualFile)
        assertNull(projectDir.findFileByRelativePath("contracts/src/Foo.tolk"))

        val renamedFile = PsiManager.getInstance(project).findFile(renamedVirtualFile!!) as TolkFile
        assertEquals("contract Bar {}", renamedFile.text)

        val actonToml = PsiManager.getInstance(project).findFile(projectDir.findChild("Acton.toml")!!)
            as TomlFile
        assertEquals(
            """
                [contracts.Bar]
                src = "contracts/src/Bar.tolk"
            """.trimIndent(),
            actonToml.text,
        )

        val script = PsiManager.getInstance(project).findFile(projectDir.findFileByRelativePath("scripts/build.tolk")!!)
        assertEquals(
            """
                fun buildContract() {
                    build("Bar");
                }
            """.trimIndent(),
            script?.text,
        )
    }

    fun `test renaming contract declared in types file updates types path`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.Foo]
                src = "contracts/Foo.boc"
                types = "contracts/Foo.types.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject("contracts/Foo.boc", "")
        myFixture.addFileToProject(
            "contracts/Foo.types.tolk",
            """
                contract Foo {}
            """.trimIndent(),
        )

        val projectDir = project.guessProjectDir()!!
        val sourceVirtualFile = projectDir.findFileByRelativePath("contracts/Foo.types.tolk")!!
        myFixture.configureFromExistingVirtualFile(sourceVirtualFile)
        val contract = PsiTreeUtil.findChildOfType(myFixture.file, TolkContractDefinition::class.java)!!

        RenameProcessor(project, contract, "Bar", false, false).run()

        val renamedVirtualFile = projectDir.findFileByRelativePath("contracts/Bar.types.tolk")
        assertNotNull(renamedVirtualFile)
        assertNull(projectDir.findFileByRelativePath("contracts/Foo.types.tolk"))

        val actonToml = PsiManager.getInstance(project).findFile(projectDir.findChild("Acton.toml")!!)
            as TomlFile
        assertEquals(
            """
                [contracts.Bar]
                src = "contracts/Foo.boc"
                types = "contracts/Bar.types.tolk"
            """.trimIndent(),
            actonToml.text,
        )
    }

    fun `test renaming contract with an existing different file name updates source path`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.Bar]
                src = "contracts/src/Counter.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "contracts/src/Counter.tolk",
            """
                contract Bar {}
            """.trimIndent(),
        )

        val projectDir = project.guessProjectDir()!!
        val sourceVirtualFile = projectDir.findFileByRelativePath("contracts/src/Counter.tolk")!!
        myFixture.configureFromExistingVirtualFile(sourceVirtualFile)
        val contract = PsiTreeUtil.findChildOfType(myFixture.file, TolkContractDefinition::class.java)!!

        RenameProcessor(project, contract, "Baz", false, false).run()

        assertNotNull(projectDir.findFileByRelativePath("contracts/src/Baz.tolk"))
        assertNull(projectDir.findFileByRelativePath("contracts/src/Counter.tolk"))

        val actonToml = PsiManager.getInstance(project).findFile(projectDir.findChild("Acton.toml")!!)
            as TomlFile
        assertEquals(
            """
                [contracts.Baz]
                src = "contracts/src/Baz.tolk"
            """.trimIndent(),
            actonToml.text,
        )
    }

    fun `test renaming contract from Acton key updates declaration and file`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.Foo]
                src = "contracts/Foo.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject("contracts/Foo.tolk", "contract Foo {}")

        val projectDir = project.guessProjectDir()!!
        val actonToml = PsiManager.getInstance(project).findFile(projectDir.findChild("Acton.toml")!!)
            as TomlFile
        val contractKey = PsiTreeUtil.findChildrenOfType(actonToml, TomlKeySegment::class.java)
            .single { it.name == "Foo" }

        RenameProcessor(project, contractKey, "Bar", false, false).run()

        assertNotNull(projectDir.findFileByRelativePath("contracts/Bar.tolk"))
        assertNull(projectDir.findFileByRelativePath("contracts/Foo.tolk"))
        assertEquals("[contracts.Bar]\nsrc = \"contracts/Bar.tolk\"", actonToml.text)

        val renamedFile = PsiManager.getInstance(project)
            .findFile(projectDir.findFileByRelativePath("contracts/Bar.tolk")!!)
        assertEquals("contract Bar {}", renamedFile?.text)
    }

    fun `test renaming contract updates all Acton entries for the same file`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.Foo]
                src = "contracts/Foo.tolk"

                [contracts.FooAlias]
                src = "contracts/Foo.tolk"
            """.trimIndent(),
        )
        myFixture.addFileToProject("contracts/Foo.tolk", "contract Foo {}")

        val projectDir = project.guessProjectDir()!!
        val sourceVirtualFile = projectDir.findFileByRelativePath("contracts/Foo.tolk")!!
        myFixture.configureFromExistingVirtualFile(sourceVirtualFile)
        val contract = PsiTreeUtil.findChildOfType(myFixture.file, TolkContractDefinition::class.java)!!

        RenameProcessor(project, contract, "Bar", false, false).run()

        val actonToml = PsiManager.getInstance(project).findFile(projectDir.findChild("Acton.toml")!!)
            as TomlFile
        assertEquals(
            """
                [contracts.Bar]
                src = "contracts/Bar.tolk"

                [contracts.FooAlias]
                src = "contracts/Bar.tolk"
            """.trimIndent(),
            actonToml.text,
        )
    }

    fun `test renaming compound contract file preserves single quote path`() {
        myFixture.addFileToProject(
            "Acton.toml",
            """
                [contracts.Foo]
                src = 'contracts/Foo.test.tolk'
            """.trimIndent(),
        )
        myFixture.addFileToProject("contracts/Foo.test.tolk", "contract Foo {}")

        val projectDir = project.guessProjectDir()!!
        val sourceVirtualFile = projectDir.findFileByRelativePath("contracts/Foo.test.tolk")!!
        myFixture.configureFromExistingVirtualFile(sourceVirtualFile)
        val contract = PsiTreeUtil.findChildOfType(myFixture.file, TolkContractDefinition::class.java)!!

        RenameProcessor(project, contract, "Bar", false, false).run()

        assertNotNull(projectDir.findFileByRelativePath("contracts/Bar.test.tolk"))
        val actonToml = PsiManager.getInstance(project).findFile(projectDir.findChild("Acton.toml")!!)
            as TomlFile
        assertEquals(
            """
                [contracts.Bar]
                src = 'contracts/Bar.test.tolk'
            """.trimIndent(),
            actonToml.text,
        )
    }

    fun `test renaming contract without Acton configuration only changes declaration`() {
        myFixture.addFileToProject("contracts/Foo.tolk", "contract Foo {}")

        val projectDir = project.guessProjectDir()!!
        val sourceVirtualFile = projectDir.findFileByRelativePath("contracts/Foo.tolk")!!
        myFixture.configureFromExistingVirtualFile(sourceVirtualFile)
        val contract = PsiTreeUtil.findChildOfType(myFixture.file, TolkContractDefinition::class.java)!!

        RenameProcessor(project, contract, "Bar", false, false).run()

        assertNull(projectDir.findFileByRelativePath("contracts/Bar.tolk"))
        assertEquals("contract Bar {}", myFixture.file.text)
    }
}
