package org.ton.intellij.tolk.codeInsight.imports

import com.intellij.lang.ImportOptimizer
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkIncludeDefinition
import org.ton.intellij.tolk.psi.impl.path
import org.ton.intellij.tolk.psi.impl.resolve

class TolkImportOptimizer : ImportOptimizer {
    override fun supports(file: PsiFile) = file is TolkFile

    override fun processFile(file: PsiFile): Runnable {
        if (file !is TolkFile) {
            return EmptyRunnable.getInstance()
        }

        val imports = file.includeDefinitions.toList()
        val importsToDelete = collectDuplicateImports(imports)
        importsToDelete += collectUnusedImports(file)

        return Runnable {
            if (importsToDelete.isNotEmpty()) {
                val manager = PsiDocumentManager.getInstance(file.project)
                val document = manager.getDocument(file)
                if (document != null) {
                    manager.commitDocument(document)
                }
            }
            for (importEntry in importsToDelete) {
                if (!importEntry.isValid) continue
                deleteImport(importEntry)
            }
        }
    }

    fun collectDuplicateImports(imports: List<TolkIncludeDefinition>): MutableSet<TolkIncludeDefinition> {
        val importsToDelete = mutableSetOf<TolkIncludeDefinition>()
        val importsSet = imports.map { it.path to it }

        for ((importPath, definition) in importsSet) {
            if (importsToDelete.contains(definition)) {
                continue
            }

            val importsWithSamePath = imports.filter { it.path == importPath }
            if (importsWithSamePath.size > 1) {
                importsWithSamePath.subList(1, importsWithSamePath.size).forEach { specToDelete ->
                    importsToDelete.add(specToDelete)
                }
            }
        }

        return importsToDelete
    }

    private fun deleteImport(import: TolkIncludeDefinition?) {
        import?.delete()
    }

    companion object {
        fun collectUnusedImports(file: TolkFile):  MutableSet<TolkIncludeDefinition> {
            val imports = file.includeDefinitions.toMutableList()
            val importsToDelete = mutableSetOf<TolkIncludeDefinition>()

            for (import in imports) {
                val importedFile = import.resolve() as? TolkFile ?: continue
                val names = mutableSetOf<String>()

                importedFile.functions.forEach { names.add(it.name ?: return@forEach) }
                importedFile.constVars.forEach { names.add(it.name ?: return@forEach) }
                importedFile.globalVars.forEach { names.add(it.name ?: return@forEach) }
                importedFile.typeDefs.forEach { names.add(it.name ?: return@forEach) }
                importedFile.structs.forEach { names.add(it.name ?: return@forEach) }
                importedFile.enums.forEach { names.add(it.name ?: return@forEach) }

                if (!usedInFile(file, names)) {
                    importsToDelete.add(import)
                }
            }

            return importsToDelete
        }

        private fun usedInFile(file: TolkFile, names: MutableSet<String>): Boolean {
            val lines = file.text.lines()
            for (line in lines) {
                for (name in names) {
                    if (line.contains("//")) {
                        // handle cases like this:
                        // ```
                        // fun foo(): Bar { // comment about Foo
                        // ```
                        val beforeComment = line.substringBefore("//")
                        if (beforeComment.contains(name)) {
                            return true
                        }
                        continue
                    }

                    if (line.contains(name)) {
                        return true
                    }
                }
            }

            return false
        }
    }
}
