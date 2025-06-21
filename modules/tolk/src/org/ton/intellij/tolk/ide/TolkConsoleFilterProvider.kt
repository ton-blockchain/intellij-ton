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
        val match = pattern.find(line) ?: return null

        val path = match.groupValues[1]
        val lineNumber = match.groupValues[2].toInt() - 1
        val column = match.groupValues[3].toInt() - 1

        val projectDir = project.basePath ?: return null
        val fullPath = File(projectDir, path).absolutePath

        val attrs = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES)

        val pathStart = match.groups[1]!!.range.first
        val columnEnd = match.groups[3]!!.range.last

        val offset = entireLength - line.length
        return Filter.Result(
            offset + pathStart,
            offset + columnEnd + 1,
            TactFileHyperlinkInfo(fullPath, lineNumber, column),
            attrs
        )
    }

    companion object {
        // Matches file paths in tact error messages
        private val pattern = Regex("""([0-9a-zA-Z_\-\\./]+\.tolk):(\d+):(\d+)""")
    }
}

class TactFileHyperlinkInfo(val path: String, val line: Int, val column: Int) : HyperlinkInfo {
    override fun navigate(project: Project) {
        val f = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(path))
        if (f != null) {
            OpenFileDescriptor(project, f, line, column).navigate(true)
        }
    }
}
