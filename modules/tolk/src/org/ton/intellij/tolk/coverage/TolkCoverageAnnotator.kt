package org.ton.intellij.tolk.coverage

import com.intellij.coverage.SimpleCoverageAnnotator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.ton.intellij.tolk.TolkBundle
import java.io.File

class TolkCoverageAnnotator(project: Project) : SimpleCoverageAnnotator(project) {
    override fun fillInfoForUncoveredFile(file: File): FileCoverageInfo = FileCoverageInfo()

    override fun getLinesCoverageInformationString(info: FileCoverageInfo): String? =
        when {
            info.totalLineCount == 0 -> null
            info.coveredLineCount == 0 -> TolkBundle.message("no.lines.covered")
            info.coveredLineCount * 100 < info.totalLineCount -> TolkBundle.message("1.lines.covered")
            else -> TolkBundle.message("0.lines.covered", calcCoveragePercentage(info))
        }

    override fun getFilesCoverageInformationString(info: DirCoverageInfo): String? =
        when {
            info.totalFilesCount == 0 -> null
            info.coveredFilesCount == 0 -> TolkBundle.message("0.of.1.files.covered", info.coveredFilesCount, info.totalFilesCount)
            else -> TolkBundle.message("0.of.1.files", info.coveredFilesCount, info.totalFilesCount)
        }

    companion object {
        fun getInstance(project: Project): TolkCoverageAnnotator = project.service()
    }
}
