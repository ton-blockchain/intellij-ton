package org.ton.intellij.tolk.coverage

import com.intellij.codeEditor.printing.ExportToHTMLSettings
import com.intellij.coverage.*
import com.intellij.coverage.view.CoverageViewExtension
import com.intellij.coverage.view.CoverageViewManager
import com.intellij.coverage.view.DirectoryCoverageViewExtension
import com.intellij.coverage.view.PercentageCoverageColumnInfo
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.AlphaComparator
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.rt.coverage.data.ClassData
import com.intellij.util.ui.ColumnInfo
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.TolkFileType
import org.ton.intellij.tolk.coverage.LcovCoverageReport.Serialization.writeLcov
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.tolk.psi.TolkFile
import java.io.File
import java.io.IOException
import java.util.*

@Suppress("UnstableApiUsage")
class TolkCoverageEngine : CoverageEngine() {
    override fun getQualifiedNames(sourceFile: PsiFile): Set<String> {
        val qName = getQName(sourceFile)
        return if (qName != null) setOf(qName) else emptySet()
    }

    override fun acceptedByFilters(psiFile: PsiFile, suite: CoverageSuitesBundle): Boolean = psiFile is TolkFile

    override fun coverageEditorHighlightingApplicableTo(psiFile: PsiFile): Boolean = psiFile is TolkFile

    override fun createCoverageEnabledConfiguration(conf: RunConfigurationBase<*>): CoverageEnabledConfiguration =
        TolkCoverageEnabledConfiguration(conf)

    override fun getQualifiedName(outputFile: File, sourceFile: PsiFile): String? = getQName(sourceFile)

    override fun includeUntouchedFileInCoverage(
        qualifiedName: String,
        outputFile: File,
        sourceFile: PsiFile,
        suite: CoverageSuitesBundle,
    ): Boolean = false

    override fun coverageProjectViewStatisticsApplicableTo(fileOrDir: VirtualFile): Boolean =
        !fileOrDir.isDirectory && fileOrDir.fileType == TolkFileType

    override fun getTestMethodName(element: PsiElement, testProxy: AbstractTestProxy): String? = null

    override fun getCoverageAnnotator(project: Project): CoverageAnnotator = TolkCoverageAnnotator.getInstance(project)

    override fun isApplicableTo(conf: RunConfigurationBase<*>): Boolean = 
        conf is ActonCommandConfiguration && conf.command == "test"

    override fun createEmptyCoverageSuite(coverageRunner: CoverageRunner): CoverageSuite = TolkCoverageSuite()

    override fun getPresentableText(): String = TolkBundle.message("action.tolk.coverage.text")

    override fun createCoverageViewExtension(
        project: Project,
        suiteBundle: CoverageSuitesBundle,
        stateBean: CoverageViewManager.StateBean,
    ): CoverageViewExtension =
        object : DirectoryCoverageViewExtension(project, getCoverageAnnotator(project), suiteBundle, stateBean) {
            override fun createColumnInfos(): Array<ColumnInfo<NodeDescriptor<*>, String>> {
                val percentage = PercentageCoverageColumnInfo(
                    1,
                    TolkBundle.message("column.name.covered"),
                    mySuitesBundle,
                )
                val files = object : ColumnInfo<NodeDescriptor<*>, String>(TolkBundle.message("column.name.file")) {
                    override fun valueOf(item: NodeDescriptor<*>?): String = item.toString()
                    override fun getComparator(): Comparator<NodeDescriptor<*>>? = AlphaComparator.INSTANCE
                }
                return arrayOf(files, percentage)
            }

            override fun getChildrenNodes(node: AbstractTreeNode<*>): List<AbstractTreeNode<*>> =
                super.getChildrenNodes(node).filter { child ->
                    val value = child.value
                    if (value is PsiFile) {
                        value.fileType == TolkFileType
                    } else {
                        child.name != Project.DIRECTORY_STORE_FOLDER
                    }
                }
        }

    override fun recompileProjectAndRerunAction(
        module: Module,
        suite: CoverageSuitesBundle,
        chooseSuiteAction: Runnable,
    ): Boolean = false

    override fun canHavePerTestCoverage(conf: RunConfigurationBase<*>): Boolean = false

    override fun findTestsByNames(testNames: Array<out String>, project: Project): List<PsiElement> = emptyList()

    override fun isReportGenerationAvailable(
        project: Project,
        dataContext: DataContext,
        currentSuite: CoverageSuitesBundle,
    ): Boolean = true

    override fun generateReport(project: Project, dataContext: DataContext, currentSuiteBundle: CoverageSuitesBundle) {
        val coverageReport = LcovCoverageReport()
        val dataManager = CoverageDataManager.getInstance(project)
        for (suite in currentSuiteBundle.suites) {
            val projectData = suite.getCoverageData(dataManager) ?: continue
            val classDataMap = projectData.classes
            for ((filePath, classData) in classDataMap) {
                val lineHitsList = convertClassDataToLineHits(classData)
                coverageReport.mergeFileReport(null, filePath, lineHitsList)
            }
        }

        val settings = ExportToHTMLSettings.getInstance(project)
        val outputDir = File(settings.OUTPUT_DIRECTORY)
        FileUtil.createDirectory(outputDir)
        val outputFileName = getOutputFileName(currentSuiteBundle)
        val title = TolkBundle.message("dialog.title.coverage.report.generation")
        try {
            val output = File(outputDir, outputFileName)
            writeLcov(coverageReport, output)
            refresh(output)
            // TODO: generate html report ourselves
            val url = "https://github.com/linux-test-project/lcov"
            Messages.showInfoMessage(
                TolkBundle.message(
                    "dialog.message.html.coverage.report.has.been.successfully.saved.as.file.br.use.instruction.in.href.to.generate.html.output.html",
                    outputFileName,
                    url,
                    url
                ),
                title
            )
        } catch (e: IOException) {
            LOG.warn("Can not export coverage data", e)
            Messages.showErrorDialog(
                TolkBundle.message(
                    "dialog.message.can.not.generate.coverage.report", e.message ?: ""
                ), title
            )
        }
    }

    private fun refresh(file: File) {
        val vFile = VfsUtil.findFileByIoFile(file, true)
        if (vFile != null) {
            runWriteAction { vFile.refresh(false, false) }
        }
    }

    private fun getOutputFileName(currentSuitesBundle: CoverageSuitesBundle): String = buildString {
        for (suite in currentSuitesBundle.suites) {
            val presentableName = suite.presentableName
            append(presentableName)
        }
        append(".lcov")
    }

    private fun convertClassDataToLineHits(classData: ClassData): List<LcovCoverageReport.LineHits> {
        val lineCount = classData.lines.size
        val lineHitsList = ArrayList<LcovCoverageReport.LineHits>(lineCount)
        for (lineInd in 0 until lineCount) {
            val lineData = classData.getLineData(lineInd)
            if (lineData != null) {
                val lineHits = LcovCoverageReport.LineHits(lineData.lineNumber, lineData.hits)
                lineHitsList.add(lineHits)
            }
        }
        return lineHitsList
    }

    override fun collectSrcLinesForUntouchedFile(classFile: File, suite: CoverageSuitesBundle): List<Int>? = null

    @Deprecated("deprecated in Java")
    override fun createCoverageSuite(
        covRunner: CoverageRunner,
        name: String,
        coverageDataFileProvider: CoverageFileProvider,
        filters: Array<out String?>?,
        lastCoverageTimeStamp: Long,
        suiteToMerge: String?,
        coverageByTestEnabled: Boolean,
        tracingEnabled: Boolean,
        trackTestFolders: Boolean,
        project: Project,
    ): CoverageSuite = TolkCoverageSuite(project, name, coverageDataFileProvider, covRunner, project.guessProjectDir()?.path)

    @Deprecated("deprecated in Java")
    override fun createCoverageSuite(
        covRunner: CoverageRunner,
        name: String,
        coverageDataFileProvider: CoverageFileProvider,
        config: CoverageEnabledConfiguration,
    ): CoverageSuite? {
        if (config !is TolkCoverageEnabledConfiguration) return null
        val configuration = config.configuration as? ActonCommandConfiguration ?: return null
        return TolkCoverageSuite(
            configuration.project,
            name,
            coverageDataFileProvider,
            covRunner,
            null,
        )
    }

    companion object {
        private val LOG: Logger = logger<TolkCoverageEngine>()

        fun getInstance(): TolkCoverageEngine = EP_NAME.findExtensionOrFail(TolkCoverageEngine::class.java)

        private fun getQName(sourceFile: PsiFile): String? = sourceFile.virtualFile?.path
    }
}
