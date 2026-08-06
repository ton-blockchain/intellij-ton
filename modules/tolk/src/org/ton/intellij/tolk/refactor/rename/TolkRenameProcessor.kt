package org.ton.intellij.tolk.refactor.rename

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlPsiFactory
import org.toml.lang.psi.TomlTable
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.psi.TolkContractDefinition
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkNamedElement
import org.ton.intellij.tolk.psi.TolkSelfParameter
import java.nio.file.Path

class TolkRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        if (element is TolkSelfParameter) {
            return false
        }
        return element is TolkNamedElement || isContractKey(element)
    }

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        val context = findRenameContext(element) ?: return
        allRenames[context.contract.element] = newName
        allRenames[context.file] = renamedFileName(context.file, newName)
        context.file.putUserData(FILE_RENAME_SCHEDULED, true)
    }

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<out UsageInfo>,
        listener: RefactoringElementListener?,
    ) {
        val context = findRenameContext(element)
        if (context == null) {
            super.renameElement(element, newName, usages, listener)
            return
        }

        val file = context.file
        val fileRenameScheduled = file.getUserData(FILE_RENAME_SCHEDULED) == true
        val oldFileName = file.name
        val pathRenames = configuredPathRenames(context.actonToml, file)

        if (element is TomlKeySegment || !fileRenameScheduled) {
            pathRenames.forEach { (literal, path) ->
                replaceSourcePath(literal, renameSourcePath(path, renamedFileName(file, newName)))
            }
        }

        super.renameElement(element, newName, usages, listener)

        if (!fileRenameScheduled) {
            if (element is TolkContractDefinition) {
                context.contract.element.setName(newName)
            } else if (element is TomlKeySegment) {
                findContractDefinition(file, context.contract.id)?.setName(newName)
            }
            if (file.isValid && file.name == oldFileName) {
                file.setName(renamedFileName(file, newName))
            }
        }
    }

    private fun findRenameContext(element: PsiElement): RenameContext? {
        if (element is TolkContractDefinition) {
            val file = element.containingFile.originalFile as? TolkFile ?: return null
            val sourceFile = file.virtualFile ?: return null
            val actonToml = ActonToml.find(element.project, sourceFile) ?: return null
            val contract = actonToml.getContracts().firstOrNull { it.id == element.name } ?: return null
            return RenameContext(actonToml, contract, file)
        }

        val key = element as? TomlKeySegment ?: return null
        if (!isContractKey(key)) {
            return null
        }
        val tomlFile = key.containingFile
        val sourceFile = tomlFile.virtualFile ?: return null
        val actonToml = ActonToml.find(key.project, sourceFile) ?: return null
        val contracts = actonToml.getContracts()
        val contract = contracts.firstOrNull { it.id == key.name } ?: return null
        val file = findContractFile(contract, actonToml) ?: return null
        return RenameContext(actonToml, contract, file)
    }

    private fun findContractFile(contract: ActonToml.ContractInfo, actonToml: ActonToml): TolkFile? {
        val paths = listOfNotNull(contract.sourcePath, contract.typesPath)
        var firstTolkFile: TolkFile? = null
        for (path in paths) {
            val virtualFile = findConfiguredFile(path, actonToml) ?: continue
            val file = PsiManager.getInstance(actonToml.project).findFile(virtualFile) as? TolkFile ?: continue
            firstTolkFile = firstTolkFile ?: file
            if (findContractDefinition(file, contract.id) != null) return file
        }
        return firstTolkFile
    }

    private fun findConfiguredFile(path: String, actonToml: ActonToml): VirtualFile? {
        if (Path.of(path).isAbsolute) {
            return LocalFileSystem.getInstance().findFileByPath(path)
        }
        return actonToml.virtualFile.parent?.findFileByRelativePath(path.replace('\\', '/'))
    }

    private fun findContractDefinition(file: TolkFile, name: String): TolkContractDefinition? =
        PsiTreeUtil.findChildrenOfType(file, TolkContractDefinition::class.java)
            .firstOrNull { it.name == name }

    private fun contractPaths(
        contract: ActonToml.ContractInfo,
        actonToml: ActonToml,
        sourceFile: VirtualFile,
    ): List<Pair<String, String>> = buildList {
        contract.sourcePath
            ?.takeIf { actonToml.isConfiguredPath(it, sourceFile) }
            ?.let { add("src" to it) }
        contract.typesPath
            ?.takeIf { actonToml.isConfiguredPath(it, sourceFile) }
            ?.let { add("types" to it) }
    }

    private fun configuredPathRenames(actonToml: ActonToml, file: TolkFile): List<Pair<TomlLiteral, String>> {
        val sourceFile = file.virtualFile ?: return emptyList()
        return actonToml.getContracts().flatMap { contract ->
            contractPaths(contract, actonToml, sourceFile).mapNotNull { (key, path) ->
                findPathLiteral(contract.element, key)?.let { it to path }
            }
        }
    }

    private fun isContractKey(element: PsiElement): Boolean {
        val key = element as? TomlKeySegment ?: return false
        val table = PsiTreeUtil.getParentOfType(key, TomlTable::class.java, false) ?: return false
        val segments = table.header.key?.segments ?: return false
        return segments.size == 2 && segments[0].name == "contracts" && segments[1] == key
    }

    private fun renamedFileName(file: TolkFile, newName: String): String {
        val suffix = file.name.substringAfter('.', missingDelimiterValue = "")
        return if (suffix.isEmpty()) newName else "$newName.$suffix"
    }

    private fun findPathLiteral(contractElement: PsiElement, key: String): TomlLiteral? {
        val table = PsiTreeUtil.getParentOfType(contractElement, TomlTable::class.java, false) ?: return null
        return table.entries
            .find { it.key.text == key }
            ?.value as? TomlLiteral
    }

    private fun renameSourcePath(sourcePath: String, newFileName: String): String {
        val separatorIndex = maxOf(sourcePath.lastIndexOf('/'), sourcePath.lastIndexOf('\\'))
        return sourcePath.substring(0, separatorIndex + 1) + newFileName
    }

    private fun replaceSourcePath(literal: TomlLiteral, sourcePath: String) {
        if (!literal.isValid) return
        val quote = literal.text.firstOrNull()?.takeIf { it == '\'' || it == '"' } ?: '"'
        val newText = "$quote$sourcePath$quote"
        literal.replace(TomlPsiFactory(literal.project, false).createLiteral(newText))
    }

    private data class RenameContext(
        val actonToml: ActonToml,
        val contract: ActonToml.ContractInfo,
        val file: TolkFile,
    )

    private companion object {
        val FILE_RENAME_SCHEDULED = Key.create<Boolean>("tolk.contract.file.rename.scheduled")
    }
}
