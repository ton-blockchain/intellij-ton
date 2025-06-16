package org.ton.intellij.tolk

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path
import java.nio.file.Paths

//
//fun fileTree(builder: FileTreeBuilder.() -> Unit): FileTree =
//    FileTree(FileTreeBuilderImpl().apply { builder() }.intoDirectory())
//
//fun fileTreeFromText(@Language("Rust") text: String, commentPrefix: String = "//"): FileTree {
//    val fileSeparator = """^\s*$commentPrefix- (\S+)\s*$""".toRegex(RegexOption.MULTILINE)
//    val fileNames = fileSeparator.findAll(text).map { it.groupValues[1] }.toList()
//    val fileTexts = fileSeparator.split(text)
//        .let {
//            check(it.first().isBlank())
//            it.drop(1)
//        }
//        .map { it.trimIndent() }
//
//    check(fileNames.size == fileTexts.size) {
//        "Have you placed `//- filename.rs` markers?"
//    }
//
//    fun fill(dir: Entry.Directory, path: List<String>, contents: String) {
//        val name = path.first()
//        if (path.size == 1) {
//            dir.children[name] = Entry.File(contents)
//        } else {
//            val childDir = dir.children.getOrPut(name) { Entry.Directory(mutableMapOf()) } as Entry.Directory
//            fill(childDir, path.drop(1), contents)
//        }
//    }
//
//    return FileTree(Entry.Directory(mutableMapOf()).apply {
//        for ((path, contents) in fileNames.map { it.split("/") }.zip(fileTexts)) {
//            fill(this, path, contents)
//        }
//    })
//}
//
//interface FileTreeBuilder {
//    fun dir(name: String, builder: FileTreeBuilder.() -> Unit)
//    fun dir(name: String, tree: FileTree)
//    fun file(name: String, code: String? = null)
//    fun symlink(name: String, targetPath: String)
//
//    fun rust(name: String, @Language("Rust") code: String) = file(name, code)
//    fun toml(name: String, @Language("TOML") code: String) = file(name, code)
//}
//
//class FileTree(val rootDirectory: Entry.Directory) {
//    fun create(project: Project, directory: VirtualFile): TestProject {
//        val files: MutableList<String> = mutableListOf()
//        val filesWithCaret: MutableList<String> = mutableListOf()
//        val filesWithSelection: MutableList<String> = mutableListOf()
//
//        fun go(dir: Entry.Directory, root: VirtualFile, parentComponents: List<String> = emptyList()) {
//            for ((name, entry) in dir.children) {
//                val components = parentComponents + name
//                when (entry) {
//                    is Entry.File -> {
//                        val vFile = root.findChild(name) ?: root.createChildData(root, name)
//                        if (entry.text != null) {
//                            VfsUtil.saveText(vFile, replaceCaretMarker(entry.text))
//                            val filePath = components.joinToString(separator = "/")
//                            files += filePath
//                            if (hasCaretMarker(entry.text) || "//^" in entry.text || "#^" in entry.text) {
//                                filesWithCaret += filePath
//                            }
//                            if (hasSelectionMarker(entry.text)) {
//                                filesWithSelection += filePath
//                            }
//                        }
//                        Unit
//                    }
//                    is Entry.Directory -> {
//                        go(entry, root.createChildDirectory(root, name), components)
//                    }
//                    is Entry.Symlink -> {
//                        check(root.fileSystem == LocalFileSystem.getInstance()) {
//                            "Symlinks are available only in LocalFileSystem"
//                        }
//                        Files.createSymbolicLink(root.pathAsPath.resolve(name), root.pathAsPath.resolve(entry.targetPath))
//                        Unit
//                    }
//                }
//            }
//        }
//
//        runWriteAction {
//            go(rootDirectory, directory)
//            fullyRefreshDirectory(directory)
//        }
//
//        return TestProject(project, directory, files, filesWithCaret, filesWithSelection)
//    }
//
//    fun assertEquals(baseDir: VirtualFile) = assertFiles(baseDir, true)
//
//    fun assertContains(baseDir: VirtualFile) = assertFiles(baseDir, false)
//
//    private fun assertFiles(baseDir: VirtualFile, failOnExtraChildren: Boolean) {
//        fun go(expected: Entry.Directory, actual: VirtualFile) {
//            val actualChildren = actual.children.associateBy { it.name }
//
//            val expectedChildrenNames = expected.children.keys
//            val actualChildrenNames = actualChildren.keys
//            val relativePath = actual.path.removePrefix(baseDir.path)
//            if (failOnExtraChildren) {
//                if (expectedChildrenNames != actualChildrenNames) {
//                    throw ComparisonFailure(
//                        "Mismatch in directory $relativePath",
//                        expectedChildrenNames.toString(),
//                        actualChildrenNames.toString()
//                    )
//                }
//            } else {
//                // Take elements that exist in the first collection and absent in the other
//                val nonExistent = expectedChildrenNames - actualChildrenNames
//                if (nonExistent.isNotEmpty()) {
//                    throw ComparisonFailure(
//                        "Missing entries at $relativePath",
//                        expectedChildrenNames.toString(),
//                        actualChildrenNames.toString()
//                    )
//                }
//            }
//
//            for ((name, entry) in expected.children) {
//                val a = actualChildren[name]!!
//                when (entry) {
//                    is Entry.File -> {
//                        kotlin.check(!a.isDirectory)
//                        val actualText = convertLineSeparators(String(a.contentsToByteArray(), UTF_8))
//                        if (entry.text != null) {
//                            Assert.assertEquals(entry.text.trimEnd(), actualText.trimEnd())
//                        }
//                        Unit
//                    }
//                    is Entry.Directory -> go(entry, a)
//                    is Entry.Symlink -> error("Symlink comparison is not supported!")
//                }
//            }
//        }
//
//        saveAllDocuments()
//        go(rootDirectory, baseDir)
//    }
//
//    fun check(fixture: CodeInsightTestFixture) {
//        fun go(dir: Entry.Directory, rootPath: String) {
//            for ((name, entry) in dir.children) {
//                val path = "$rootPath/$name"
//                when (entry) {
//                    is Entry.File -> {
//                        val text = entry.text
//                        if (text == null) {
//                            if (fixture.findFileInTempDir(path) == null) error("No file at $path")
//                            Unit
//                        } else {
//                            fixture.checkResult(path, text, true)
//                        }
//                    }
//                    is Entry.Directory -> go(entry, path)
//                    is Entry.Symlink -> error("Symlink comparison is not supported!")
//                }
//            }
//        }
//
//        go(rootDirectory, ".")
//    }
//}
//
//fun FileTree.create(fixture: CodeInsightTestFixture): TestProject =
//    create(fixture.project, fixture.findFileInTempDir("."))
//
//fun FileTree.createAndOpenFileWithCaretMarker(fixture: CodeInsightTestFixture): TestProject {
//    val testProject = create(fixture)
//    fixture.configureFromTempProjectFile(testProject.fileWithCaret)
//    return testProject
//}
//
//class TestProject(
//    private val project: Project,
//    val root: VirtualFile,
//    val files: List<String>,
//    private val filesWithCaret: List<String>,
//    private val filesWithSelection: List<String>
//) {
//
//    val fileWithCaret: String
//        get() = when (filesWithCaret.size) {
//            1 -> filesWithCaret.single()
//            0 -> error("Please, add `/*caret*/` or `<caret>` marker to some file")
//            else -> error("More than one file with carets found: $filesWithCaret")
//        }
//
//    val fileWithCaretOrSelection: String get() = filesWithCaret.singleOrNull() ?: fileWithSelection
//
//    val fileWithSelection: String get() = filesWithSelection.single()
//
//    inline fun <reified T : PsiElement> findElementInFile(path: String): T {
//        return doFindElementInFile(path, T::class.java)
//    }
//
//    fun <T : PsiElement> doFindElementInFile(path: String, psiClass: Class<T>): T {
//        val file = file(path).toPsiFile(project)!!
//        return findElementInFile(file, psiClass, "^")
//    }
//
//    private fun <T : PsiElement> findElementInFile(file: PsiFile, psiClass: Class<T>, marker: String): T {
//        val (element, data, _) = findElementWithDataAndOffsetInFile(file, psiClass, marker)
//        check(data.isEmpty()) { "Did not expect marker data" }
//        return element
//    }
//
//    private fun <T : PsiElement> findElementWithDataAndOffsetInFile(
//        file: PsiFile,
//        psiClass: Class<T>,
//        marker: String
//    ): Triple<T, String, Int> {
//        val elementsWithDataAndOffset = findElementsWithDataAndOffsetInFile(file, psiClass, marker)
//        check(elementsWithDataAndOffset.isNotEmpty()) { "No `$marker` marker:\n${file.text}" }
//        check(elementsWithDataAndOffset.size <= 1) { "More than one `$marker` marker:\n${file.text}" }
//        return elementsWithDataAndOffset.first()
//    }
//
//    private fun <T : PsiElement> findElementsWithDataAndOffsetInFile(
//        file: PsiFile,
//        psiClass: Class<T>,
//        marker: String
//    ): List<Triple<T, String, Int>> {
//        return findElementsWithDataAndOffsetInEditor(
//            file,
//            file.document!!,
//            followMacroExpansions = true,
//            psiClass,
//            marker
//        )
//    }
//
//    fun psiFile(path: String): PsiFileSystemItem {
//        val vFile = file(path)
//        val psiManager = PsiManager.getInstance(project)
//        return if (vFile.isDirectory) psiManager.findDirectory(vFile)!! else psiManager.findFile(vFile)!!
//    }
//
//    fun file(path: String): VirtualFile {
//        return root.findFileByRelativePath(path) ?: error("Can't find `$path`")
//    }
//}
//
//
//private class FileTreeBuilderImpl(val directory: MutableMap<String, Entry> = mutableMapOf()) : FileTreeBuilder {
//    override fun dir(name: String, builder: FileTreeBuilder.() -> Unit) {
//        check('/' !in name) { "Bad directory name `$name`" }
//        directory[name] = FileTreeBuilderImpl().apply { builder() }.intoDirectory()
//    }
//
//    override fun dir(name: String, tree: FileTree) {
//        check('/' !in name) { "Bad directory name `$name`" }
//        directory[name] = tree.rootDirectory
//    }
//
//    override fun file(name: String, code: String?) {
//        check('/' !in name) { "Bad file name `$name`" }
//        directory[name] = Entry.File(code?.trimIndent())
//    }
//
//    override fun symlink(name: String, targetPath: String) {
//        directory[name] = Entry.Symlink(targetPath)
//    }
//
//    fun intoDirectory() = Entry.Directory(directory)
//}
//
//sealed class Entry {
//    class File(val text: String?) : Entry()
//    class Directory(val children: MutableMap<String, Entry>) : Entry()
//    class Symlink(val targetPath: String) : Entry()
//}

val VirtualFile.pathAsPath: Path get() = Paths.get(path)
fun fullyRefreshDirectory(directory: VirtualFile) {
    VfsUtil.markDirtyAndRefresh(/* async = */ false, /* recursive = */ true, /* reloadChildren = */ true, directory)
}

fun saveAllDocuments() = FileDocumentManager.getInstance().saveAllDocuments()

fun replaceCaretMarker(text: String): String = text.replace("/*caret*/", "<caret>")
fun hasCaretMarker(text: String): Boolean = text.contains("/*caret*/") || text.contains("<caret>")
fun replaceSelectionMarker(text: String): String = text
    .replace("/*selection*/", "<selection>")
    .replace("/*selection**/", "</selection>")

fun hasSelectionMarker(text: String): Boolean = text.contains("<selection>") && text.contains("</selection>")
