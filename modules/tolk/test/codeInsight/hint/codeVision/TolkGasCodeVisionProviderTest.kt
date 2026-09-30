package org.ton.intellij.tolk.codeInsight.hint.codeVision

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingProvider
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.acton.ide.setTomlPluginInstalledOverride
import org.ton.intellij.acton.settings.actonSettings
import org.ton.intellij.tolk.TolkTestBase
import org.ton.intellij.tolk.ide.assembly.TolkAssemblyPreviewBlock
import org.ton.intellij.tolk.ide.assembly.TolkAssemblyPreviewFileEditorProvider
import org.ton.intellij.tolk.ide.assembly.TolkAssemblyPreviewVirtualFile
import org.ton.intellij.tolk.psi.TolkFile
import java.nio.file.Files
import java.nio.file.Path

class TolkGasCodeVisionProviderTest : TolkTestBase() {
    private lateinit var cliDirectory: Path
    private var previousActonPath: String? = null
    private val provider = TolkGasCodeVisionProvider()

    override fun createTempDirTestFixture(): TempDirTestFixture = TempDirTestFixtureImpl()

    override fun setUp() {
        super.setUp()
        ModuleRootModificationUtil.addContentRoot(module, myFixture.tempDirFixture.tempDirPath)
        setTomlPluginInstalledOverride(true)
        previousActonPath = project.actonSettings.actonPath
        cliDirectory = Files.createTempDirectory("acton-gas-code-vision-")
    }

    override fun tearDown() {
        try {
            setTomlPluginInstalledOverride(null)
            project.actonSettings.actonPath = previousActonPath
            cliDirectory.toFile().deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun `test gas settings group has a visible name and description`() {
        val settings = ExtensionPointName.create<CodeVisionGroupSettingProvider>(
            "com.intellij.config.codeVisionGroupSettingProvider",
        ).extensionList.single { it.groupId == provider.id }
        assertEquals(provider.id, settings.groupId)
        assertEquals("Tolk gas estimates", settings.groupName)
        assertTrue(settings.description.contains("Acton"))
    }

    fun `test batches ten functions caches results and opens assembly`() {
        configure((1..10).joinToString("\n") { "fun function$it(x: int): int { return x + $it; }" })
        installCli((1..10).map { "function$it" })

        val lenses = awaitLenses(10)
        assertEquals(List(10) { "~18 gas" }, lenses.map { (_, entry) -> (entry as ClickableTextCodeVisionEntry).text })
        repeat(5) { assertEquals(10, compute().size) }
        val arguments = Files.readAllLines(cliDirectory.resolve("calls"))
        assertEquals(1, arguments.count { it == "BEGIN" })
        assertEquals(10, arguments.count { it == "--function" })
        assertTrue(arguments[1].replace('\\', '/').endsWith("/packages/app"))
        assertEquals((1..10).map { "function$it" }, arguments.filter { it.startsWith("function") })

        val entry = lenses.first().second as ClickableTextCodeVisionEntry
        entry.onClick(myFixture.editor)
        assertFalse(entry.tooltip.contains("INC"))
        val preview = FileEditorManager.getInstance(
            project,
        ).openFiles.filterIsInstance<TolkAssemblyPreviewVirtualFile>().single()
        val splitProvider = ExtensionPointName.create<FileEditorProvider>("com.intellij.fileEditorProvider")
            .extensionList.filterIsInstance<TolkAssemblyPreviewFileEditorProvider>().single()
        assertTrue(splitProvider.accept(project, preview))
        assertEquals(listOf(TolkAssemblyPreviewBlock(0..0, listOf(1..1))), preview.presentation.blocks)
        assertEquals("function1", preview.functionName)
        assertFalse(preview.isWritable)
        val assembly = FileDocumentManager.getInstance().getDocument(preview.assemblyFile)!!.text
        assertTrue(assembly.contains("INC"))
        assertFalse(assembly.contains("Click to view"))
    }

    fun `test skips unsupported declarations and explains conditional estimates`() {
        configure(
            """
                fun onInternalMessage() {}
                fun onExternalMessage(in: slice) {}
                fun regular(): int { return 1; }
                get fun getter(): int { return 2; }
                fun generic<T>(x: T): T { return x; }
                fun int.method(self): int { return self; }
                fun mutateArg(mutate x: int) { x += 1; }
                fun asmFunction(): int asm "ONE";
                fun onBouncedMessage() {}
            """.trimIndent(),
        )
        installCli(listOf("onInternalMessage", "onExternalMessage", "regular", "getter"), dynamic = true)

        val lenses = awaitLenses(4)
        val arguments = Files.readAllLines(cliDirectory.resolve("calls"))
        assertEquals(listOf("regular", "getter"), arguments.filter { it == "regular" || it == "getter" })
        assertEquals(4, arguments.count { it == "--function" })
        val tooltip = lenses.first().second.tooltip
        assertTrue(tooltip.contains("runtime data"))
        assertTrue(tooltip.contains("loop iteration"))
        assertTrue(tooltip.contains("unknown cost"))
    }

    fun `test hides unsaved source and recompiles after saving an import`() {
        val dependency = myFixture.addFileToProject("packages/app/helper.tolk", "fun helper(): int { return 1; }")
        configure("import \"../helper\"\nfun regular(): int { return helper(); }")
        installCli(listOf("regular"))
        awaitLenses(1)

        val documents = FileDocumentManager.getInstance()
        val dependencyDocument = documents.getDocument(dependency.virtualFile)!!
        WriteCommandAction.runWriteCommandAction(project) {
            dependencyDocument.setText("fun helper(): int { return 2; }")
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        assertEmpty(compute())
        documents.saveDocument(dependencyDocument)
        awaitLenses(1)
        assertEquals(2, Files.readAllLines(cliDirectory.resolve("calls")).count { it == "BEGIN" })

        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.insertString(0, "// unsaved\n")
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        assertEmpty(compute())
    }

    fun `test caches CLI errors without rerunning on every daemon pass`() {
        configure("fun regular(): int { return 1; }")
        installCli(emptyList(), success = false)
        assertEmpty(compute())
        PlatformTestUtil.waitWithEventsDispatching(
            "Acton was not invoked",
            { Files.exists(cliDirectory.resolve("calls")) },
            10,
        )
        PlatformTestUtil.waitForAlarm(800)
        repeat(5) { assertEmpty(compute()) }
        PlatformTestUtil.waitForAlarm(800)
        assertEquals(1, Files.readAllLines(cliDirectory.resolve("calls")).count { it == "BEGIN" })
    }

    private fun configure(source: String) {
        myFixture.addFileToProject("packages/app/Acton.toml", "")
        val file = myFixture.addFileToProject("packages/app/contracts/gas.tolk", source)
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        FileDocumentManager.getInstance().saveAllDocuments()
        assertTrue(file.virtualFile.isInLocalFileSystem)
        assertNotNull("Missing Acton config", ActonToml.find(project, file.virtualFile))
        assertTrue((myFixture.file as TolkFile).functions.isNotEmpty())
        assertEmpty(FileDocumentManager.getInstance().unsavedDocuments.toList())
    }

    private fun compute(): List<Pair<TextRange, CodeVisionEntry>> = provider.computeForEditor(myFixture.editor, Unit)

    private fun awaitLenses(count: Int): List<Pair<TextRange, CodeVisionEntry>> {
        PlatformTestUtil.waitWithEventsDispatching("Gas lenses did not appear", { compute().size == count }, 10)
        return compute()
    }

    private fun installCli(names: List<String>, dynamic: Boolean = false, success: Boolean = true) {
        val functions = names.mapIndexed { index, name ->
            """{"name":"$name","method_id":262143,"assembly":"INC\n","gas_estimate":{
                "value":18,"has_dynamic_cost":$dynamic,"has_control_flow":$dynamic,
                "unknown_instructions":${if (dynamic) 1 else 0}},"blocks":[{
                "source":{"file":"${myFixture.file.virtualFile.path}","line":${index + 1},
                "column":1,"end_line":${index + 1},"end_column":10},
                "assembly_ranges":[{"start_line":0,"end_line":0}]}]}"""
        }.joinToString(",")
        val script = cliDirectory.resolve("acton")
        Files.writeString(
            script,
            """
                |#!/bin/sh
                |{
                |  printf '%s\n' BEGIN
                |  pwd
                |  printf '%s\n' "${'$'}@"
                |} >> '${cliDirectory.resolve("calls")}'
                |cat <<'JSON'
                |{"success":$success,"functions":[$functions]}
                |JSON
            """.trimMargin(),
        )
        assertTrue(script.toFile().setExecutable(true))
        project.actonSettings.actonPath = script.toString()
    }
}
