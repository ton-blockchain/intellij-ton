package org.ton.intellij.tolk.ide

import com.intellij.execution.filters.ConsoleFilterProviderEx
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.search.GlobalSearchScope
import java.io.File
import kotlin.io.path.Path

class TolkConsoleFilterProvider : ConsoleFilterProviderEx {
    override fun getDefaultFilters(project: Project): Array<out Filter?> =
        getDefaultFilters(project, GlobalSearchScope.allScope(project))

    override fun getDefaultFilters(
        project: Project,
        scope: GlobalSearchScope
    ): Array<out Filter?> =
        arrayOf(TolkConsoleFilter(project, scope))
}

class TolkConsoleFilter(val project: Project, val scope: GlobalSearchScope) : Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        // Try to match Tolk test output format: "at /path/to/file.tolk:line"
        val testMatch = testPattern.find(line)
        if (testMatch != null) {
            val path = testMatch.groupValues[1]
            val lineNumber = testMatch.groupValues[2].toInt() - 1

            val attrs = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES)

            val pathStart = testMatch.groups[1]!!.range.first
            val lineEnd = testMatch.groups[2]!!.range.last

            val offset = entireLength - line.length
            return Filter.Result(
                offset + pathStart,
                offset + lineEnd + 1,
                TolkFileHyperlinkInfo(path, lineNumber, 0),
                attrs
            )
        }

        // Try to match Tolk compiler error format: "path.tolk:line:column"
        val compilerMatch = compilerPattern.find(line)
        if (compilerMatch != null) {
            val path = compilerMatch.groupValues[1]
            val lineNumber = compilerMatch.groupValues[2].toInt() - 1
            val column = compilerMatch.groupValues[3].toInt() - 1

            val projectDir = project.basePath ?: return null
            val fullPath = if (Path(path).isAbsolute) path else File(projectDir, path).absolutePath

            val attrs = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES)

            val pathStart = compilerMatch.groups[1]!!.range.first
            val columnEnd = compilerMatch.groups[3]!!.range.last

            val offset = entireLength - line.length
            return Filter.Result(
                offset + pathStart,
                offset + columnEnd + 1,
                TolkFileHyperlinkInfo(fullPath, lineNumber, column),
                attrs
            )
        }

        return null
    }

    companion object {
        // Matches file paths in Tolk test output: "at /path/to/file.tolk:line"
        private val testPattern = Regex("""at\s+(/[^\s:]+):(\d+)""")

        // Matches file paths in Tolk compiler error messages: "path.tolk:line:column"
        private val compilerPattern = Regex("""([0-9a-zA-Z_\-\\./]+\.tolk):(\d+):(\d+)""")
    }
}

class TolkFileHyperlinkInfo(val path: String, val line: Int, val column: Int) : HyperlinkInfo {
    override fun navigate(project: Project) {
        val f = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(path))
        if (f != null) {
            OpenFileDescriptor(project, f, line, column).navigate(true)
        }
    }
}
