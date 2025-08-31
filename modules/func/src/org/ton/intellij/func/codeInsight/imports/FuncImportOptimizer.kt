package org.ton.intellij.func.codeInsight.imports

import com.intellij.lang.ImportOptimizer
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.FuncIncludeDefinition
import org.ton.intellij.func.psi.impl.path

class FuncImportOptimizer : ImportOptimizer {
    override fun supports(file: PsiFile) = file is FuncFile

    override fun processFile(file: PsiFile): Runnable {
        if (file !is FuncFile) {
            return EmptyRunnable.getInstance()
        }

        val includes = file.includeDefinitions.toList()
        val includesToDelete = collectDuplicateIncludes(includes)
        includesToDelete += collectUnusedIncludes(file)

        return Runnable {
            if (includesToDelete.isNotEmpty()) {
                val manager = PsiDocumentManager.getInstance(file.project)
                val document = manager.getDocument(file)
                if (document != null) {
                    manager.commitDocument(document)
                }
            }
            for (includeEntry in includesToDelete) {
                if (!includeEntry.isValid) continue
                deleteInclude(includeEntry)
            }
        }
    }

    fun collectDuplicateIncludes(includes: List<FuncIncludeDefinition>): MutableSet<FuncIncludeDefinition> {
        val includesToDelete = mutableSetOf<FuncIncludeDefinition>()
        val includesSet = includes.map { it.path to it }

        for ((includePath, definition) in includesSet) {
            if (includesToDelete.contains(definition)) {
                continue
            }

            val includesWithSamePath = includes.filter { it.path == includePath }
            if (includesWithSamePath.size > 1) {
                includesWithSamePath.subList(1, includesWithSamePath.size).forEach { specToDelete ->
                    includesToDelete.add(specToDelete)
                }
            }
        }

        return includesToDelete
    }

    private fun deleteInclude(include: FuncIncludeDefinition?) {
        include?.delete()
    }

    companion object {
        fun collectUnusedIncludes(file: FuncFile): MutableSet<FuncIncludeDefinition> {
            val includes = file.includeDefinitions.toMutableList()
            val includesToDelete = mutableSetOf<FuncIncludeDefinition>()

            val dependencyGraph = buildDependencyGraph(file)
            val filesImportingThis = findFilesImporting(file)

            for (include in includes) {
                val includedFile = include.reference?.resolve() as? FuncFile ?: continue

                if (includedFile.name == "stdlib.fc" || includedFile.name == "stdlib.func") {
                    continue
                }

                if (!isIncludeUsed(file, includedFile, dependencyGraph, filesImportingThis)) {
                    includesToDelete.add(include)
                }
            }

            return includesToDelete
        }

        private fun findFilesImporting(targetFile: FuncFile): Set<FuncFile> {
            val importingFiles = mutableSetOf<FuncFile>()
            val project = targetFile.project

            val allFuncFiles = getAllFuncFilesInProject(project)

            for (file in allFuncFiles) {
                if (file == targetFile) continue

                for (include in file.includeDefinitions) {
                    val includedFile = include.reference?.resolve() as? FuncFile
                    if (includedFile == targetFile) {
                        importingFiles.add(file)
                        break
                    }
                }
            }

            return importingFiles
        }

        private fun getAllFuncFilesInProject(project: com.intellij.openapi.project.Project): List<FuncFile> {
            val files = mutableListOf<FuncFile>()

            val psiManager = PsiManager.getInstance(project)
            val fileIndex = ProjectRootManager.getInstance(project).fileIndex

            fileIndex.iterateContent { virtualFile ->
                if (virtualFile.extension == "fc" || virtualFile.extension == "func") {
                    val psiFile = psiManager.findFile(virtualFile)
                    if (psiFile is FuncFile) {
                        files.add(psiFile)
                    }
                }
                true
            }

            return files
        }

        private data class DependencyGraph(
            val fileToSymbols: Map<FuncFile, Set<String>>,
            val fileToTransitiveSymbols: Map<FuncFile, Set<String>>,
        )

        private fun buildDependencyGraph(rootFile: FuncFile): DependencyGraph {
            val fileToSymbols = mutableMapOf<FuncFile, Set<String>>()
            val fileToTransitiveSymbols = mutableMapOf<FuncFile, Set<String>>()
            val visited = mutableSetOf<FuncFile>()

            collectAllFilesRecursive(rootFile, visited)

            for (file in visited) {
                fileToSymbols[file] = collectDirectSymbols(file)
            }

            for (file in visited) {
                fileToTransitiveSymbols[file] = computeTransitiveSymbols(file, fileToSymbols, mutableMapOf())
            }

            return DependencyGraph(fileToSymbols, fileToTransitiveSymbols)
        }

        private fun collectAllFilesRecursive(file: FuncFile, visited: MutableSet<FuncFile>) {
            if (visited.contains(file)) return
            visited.add(file)

            for (include in file.includeDefinitions) {
                val includedFile = include.reference?.resolve() as? FuncFile ?: continue
                collectAllFilesRecursive(includedFile, visited)
            }
        }

        private fun collectDirectSymbols(file: FuncFile): Set<String> {
            val symbols = mutableSetOf<String>()

            file.functions.forEach { function ->
                function.identifier.text?.let { symbols.add(it) }
            }
            file.constVars.forEach { constVar ->
                constVar.identifier.text?.let { symbols.add(it) }
            }
            file.globalVars.forEach { globalVar ->
                globalVar.identifier?.text?.let { symbols.add(it) }
            }

            return symbols
        }

        private fun computeTransitiveSymbols(
            file: FuncFile,
            fileToSymbols: Map<FuncFile, Set<String>>,
            memo: MutableMap<FuncFile, Set<String>>,
        ): Set<String> {
            memo[file]?.let { return it }

            val allSymbols = mutableSetOf<String>()

            fileToSymbols[file]?.let { allSymbols.addAll(it) }

            for (include in file.includeDefinitions) {
                val includedFile = include.reference?.resolve() as? FuncFile ?: continue

                if (includedFile != file) {
                    val transitiveSymbols = computeTransitiveSymbols(includedFile, fileToSymbols, memo)
                    allSymbols.addAll(transitiveSymbols)
                }
            }

            memo[file] = allSymbols
            return allSymbols
        }

        private fun isIncludeUsed(
            mainFile: FuncFile,
            includedFile: FuncFile,
            graph: DependencyGraph,
            filesImportingMain: Set<FuncFile>,
        ): Boolean {
            val availableSymbols = graph.fileToTransitiveSymbols[includedFile] ?: emptySet()
            if (availableSymbols.isEmpty()) return false

            if (usedInFile(mainFile, availableSymbols)) {
                return true
            }

            for (importingFile in filesImportingMain) {
                if (usedInFile(importingFile, availableSymbols)) {
                    return true
                }
            }

            return false
        }

        private fun usedInFile(file: FuncFile, names: Set<String>): Boolean {
            if (names.isEmpty()) return false

            val lines = file.text.lines()
            for (line in lines) {
                val effectiveLine = if (line.contains(";;")) {
                    line.substringBefore(";;")
                } else if (line.contains("{-")) {
                    line.substringBefore("{-")
                } else {
                    line
                }

                for (name in names) {
                    if (effectiveLine.contains(name)) {
                        return true
                    }
                }
            }

            return false
        }
    }
}
