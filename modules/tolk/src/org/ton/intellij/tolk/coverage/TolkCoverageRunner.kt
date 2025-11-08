package org.ton.intellij.tolk.coverage

import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageRunner
import com.intellij.coverage.CoverageSuite
import com.intellij.ide.plugins.PluginManagerCore.isUnitTestMode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.ProgressTitle
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.coverage.LcovCoverageReport.Serialization.readLcov
import java.io.File
import java.io.IOException

fun <T> Project.computeWithCancelableProgress(
    @Suppress("UnstableApiUsage") @ProgressTitle title: String,
    supplier: () -> T
): T {
    if (isUnitTestMode) {
        return supplier()
    }
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<T, Exception>(supplier, title, true, this)
}

class TolkCoverageRunner : CoverageRunner() {
    override fun getPresentableName(): String = "Tolk"

    override fun getDataFileExtension(): String = "info"

    override fun getId(): String = "TolkCoverageRunner"

    override fun acceptsCoverageEngine(engine: CoverageEngine): Boolean = engine is TolkCoverageEngine

    override fun loadCoverageData(sessionDataFile: File, baseCoverageSuite: CoverageSuite?): ProjectData? {
        if (baseCoverageSuite !is TolkCoverageSuite) return null
        return try {
            if (ApplicationManager.getApplication().isDispatchThread) {
                baseCoverageSuite.project.computeWithCancelableProgress(TolkBundle.message("progress.title.loading.coverage.data")) {
                    readProjectData(sessionDataFile, baseCoverageSuite)
                }
            } else {
                readProjectData(sessionDataFile, baseCoverageSuite)
            }
        } catch (e: IOException) {
            LOG.warn("Can't read coverage data", e)
            null
        }
    }

    companion object {
        private val LOG: Logger = logger<TolkCoverageRunner>()

        @Throws(IOException::class)
        private fun readProjectData(dataFile: File, coverageSuite: TolkCoverageSuite): ProjectData? {
            val projectData = ProjectData()
            val report = readLcov(dataFile, coverageSuite.contextFilePath)
            for ((filePath, lineHitsList) in report.records) {
                val classData = projectData.getOrCreateClassData(filePath)
                val max = lineHitsList.lastOrNull()?.lineNumber ?: 0
                val lines = arrayOfNulls<LineData>(max + 1)
                for (lineHits in lineHitsList) {
                    val lineData = LineData(lineHits.lineNumber, null)
                    lineData.hits = lineHits.hits
                    lines[lineHits.lineNumber] = lineData
                }
                classData.setLines(lines)
            }
            return projectData
        }
    }
}