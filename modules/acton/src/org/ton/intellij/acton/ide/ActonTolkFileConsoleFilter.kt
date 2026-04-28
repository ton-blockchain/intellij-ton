package org.ton.intellij.acton.ide

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import kotlin.io.path.Path

class ActonTolkFileConsoleFilter(private val project: Project) : Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val match = pattern.find(line) ?: return null

        val pathText = match.groupValues[1]
        val lineNumber = match.groupValues[2].toInt() - 1
        val column = match.groupValues[3].toInt() - 1
        val projectDir = project.basePath ?: return null
        val resolvedPath = if (Path(pathText).isAbsolute) pathText else File(projectDir, pathText).absolutePath

        val attrs = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES)
        val lineStart = entireLength - line.length
        val pathStart = match.groups[1]!!.range.first
        val columnEnd = match.groups[3]!!.range.last

        return Filter.Result(
            lineStart + pathStart,
            lineStart + columnEnd + 1,
            ActonTolkFileHyperlinkInfo(resolvedPath, lineNumber, column),
            attrs,
        )
    }

    private class ActonTolkFileHyperlinkInfo(
        private val path: String,
        private val line: Int,
        private val column: Int,
    ) : HyperlinkInfo {
        override fun navigate(project: Project) {
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(path))
                ?: return
            OpenFileDescriptor(project, virtualFile, line, column).navigate(true)
        }
    }

    private companion object {
        private val pattern = Regex("""([0-9a-zA-Z_\-\\./]+\.tolk):(\d+):(\d+)""")
    }
}
