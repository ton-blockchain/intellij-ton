package org.ton.intellij.acton.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.ton.intellij.acton.cli.ActonToml
import java.io.File
import java.nio.file.Path

internal enum class ActonWrapperLanguage(
    val fileSuffix: String,
    val configName: String,
    val defaultOutputDir: String,
) {
    TOLK(".gen.tolk", "tolk", "wrappers"),
    TYPESCRIPT(".gen.ts", "typescript", "wrappers-ts"),
    ;

    companion object {
        fun fromFile(file: VirtualFile): ActonWrapperLanguage? =
            entries.firstOrNull { file.name.endsWith(it.fileSuffix) }
    }
}

internal data class ActonWrapperTarget(
    val actonToml: ActonToml,
    val contractId: String,
    val language: ActonWrapperLanguage,
    val wrapperFile: VirtualFile,
    val sourceFile: VirtualFile,
    val typesFile: VirtualFile?,
)

internal fun findActonWrapperTarget(project: Project, file: VirtualFile): ActonWrapperTarget? {
    val language = ActonWrapperLanguage.fromFile(file) ?: return null
    val actonToml = ActonToml.find(project, file) ?: return null
    val wrapperName = file.name.removeSuffix(language.fileSuffix).takeIf(String::isNotBlank) ?: return null
    val expectedWrapperPath = runCatching {
        expectedWrapperPath(actonToml, wrapperName, language)
    }.getOrNull() ?: return null
    if (normalizePath(file.path) != normalizePath(expectedWrapperPath.toString())) return null

    val matches = actonToml.getContracts().mapNotNull { contract ->
        val sourcePath = contract.sourcePath ?: return@mapNotNull null
        val sourceFile = findConfiguredFile(actonToml, sourcePath) ?: return@mapNotNull null
        val sourceStem = sourceFile.nameWithoutExtension.toPascalCase()
        if (sourceStem != wrapperName) return@mapNotNull null

        val typesFile = contract.typesPath?.let { findConfiguredFile(actonToml, it) }
        ActonWrapperTarget(actonToml, contract.id, language, file, sourceFile, typesFile)
    }

    return matches.singleOrNull()
}

private fun expectedWrapperPath(actonToml: ActonToml, wrapperName: String, language: ActonWrapperLanguage): Path {
    val configuredOutputDir = actonToml.getWrapperOutputDir(language.configName)
        ?.takeIf(String::isNotBlank)
    val outputDir = when {
        configuredOutputDir != null -> Path.of(actonToml.resolveConfiguredPath(configuredOutputDir))
        language == ActonWrapperLanguage.TOLK -> {
            val mappedOutputDir = actonToml.getNormalizedMappings()["wrappers"]
            mappedOutputDir?.let(Path::of) ?: actonToml.workingDir.resolve(language.defaultOutputDir)
        }

        else -> actonToml.workingDir.resolve(language.defaultOutputDir)
    }

    return outputDir.resolve(wrapperName + language.fileSuffix).normalize()
}

private fun findConfiguredFile(actonToml: ActonToml, configuredPath: String): VirtualFile? {
    val resolvedPath = runCatching {
        Path.of(actonToml.resolveConfiguredPath(configuredPath)).normalize()
    }.getOrNull() ?: return null
    val root = actonToml.virtualFile.parent ?: return null
    val rootPath = Path.of(actonToml.workingDir.toString()).normalize()
    val relativePath = runCatching { rootPath.relativize(resolvedPath) }.getOrNull()
    if (relativePath != null && !relativePath.startsWith("..")) {
        return VfsUtilCore.findRelativeFile(relativePath.toString().replace(File.separatorChar, '/'), root)
    }

    return LocalFileSystem.getInstance().findFileByPath(resolvedPath.toString())
}

private fun normalizePath(path: String): String = path.replace('\\', '/').trimEnd('/')

private fun String.toPascalCase(): String {
    val result = StringBuilder(length)
    var capitalizeNext = true
    for (character in this) {
        if (character == '_' || character == '-') {
            capitalizeNext = true
        } else if (capitalizeNext) {
            result.append(character.uppercaseChar())
            capitalizeNext = false
        } else {
            result.append(character)
        }
    }
    return result.takeIf { it.isNotEmpty() }?.toString() ?: this
}
