package org.ton.intellij.acton.ide

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import org.ton.intellij.acton.cli.ActonToml
import java.io.File
import java.nio.file.Path

class ActonGeneratedFileConsoleFilter(private val project: Project) : Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val extractedPath = extractGeneratedPath(line) ?: return null
        val resolvedPath = resolvePath(extractedPath.pathText) ?: return null
        if (!resolvedPath.isFile) {
            return null
        }

        val attrs = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES)
        val lineStart = entireLength - line.length

        return Filter.Result(
            lineStart + extractedPath.startOffset,
            lineStart + extractedPath.endOffset,
            ActonGeneratedFileHyperlinkInfo(resolvedPath.path),
            attrs,
        )
    }

    private fun extractGeneratedPath(line: String): GeneratedPath? {
        if (!line.startsWith(GENERATED_PREFIX)) {
            return null
        }

        val rawSuffix = line.substring(GENERATED_PREFIX.length)
        val pathText = rawSuffix.trim()
        if (pathText.isEmpty()) {
            return null
        }
        val pathStart = GENERATED_PREFIX.length + rawSuffix.indexOf(pathText)
        return GeneratedPath(pathText, pathStart, pathStart + pathText.length)
    }

    private fun resolvePath(pathText: String): File? {
        val path = try {
            Path.of(pathText)
        } catch (_: Exception) {
            return null
        }
        if (path.isAbsolute) {
            return path.toFile()
        }

        val candidates = buildList {
            ActonToml.find(project)?.workingDir?.let { add(it.resolve(path).toFile()) }
            project.basePath?.let { add(File(it, pathText)) }
        }

        return candidates.firstOrNull { it.exists() }
    }

    private class ActonGeneratedFileHyperlinkInfo(private val path: String) : HyperlinkInfo {
        override fun navigate(project: Project) {
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(path))
                ?: return
            OpenFileDescriptor(project, virtualFile).navigate(true)
        }
    }

    private companion object {
        private const val GENERATED_PREFIX = "   Generated"
    }

    private data class GeneratedPath(val pathText: String, val startOffset: Int, val endOffset: Int)
}
