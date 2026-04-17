package org.ton.intellij.tolk.coverage

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.runners.executeState
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import org.ton.intellij.acton.runconfig.ACTON_COVERAGE_EXECUTOR_ID
import org.ton.intellij.acton.runconfig.ACTON_COVERAGE_UNSUPPORTED_MESSAGE
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.isActonCoverageSupported
import java.io.File

class TolkCoverageProgramRunner : GenericProgramRunner<RunnerSettings>() {
    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean = executorId == ACTON_COVERAGE_EXECUTOR_ID &&
        profile is ActonCommandConfiguration &&
        profile.command == "test"

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (!isActonCoverageSupported()) {
            throw ExecutionException(ACTON_COVERAGE_UNSUPPORTED_MESSAGE)
        }
        val executeState = executeState(state, environment, this)
        executeState?.processHandler?.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                val workingDir = environment.project.guessProjectDir()?.toNioPath()?.toFile() ?: return
                val coverageFile = File(workingDir, "lcov.info")
                registerCoverage(environment.project, coverageFile)
            }
        })
        return executeState
    }

    private fun registerCoverage(project: Project, coverageFile: File) {
        val dataManagerClass = Class.forName("com.intellij.coverage.CoverageDataManager")
        val coverageRunnerClass = Class.forName("com.intellij.coverage.CoverageRunner")
        val coverageSuiteClass = Class.forName("com.intellij.coverage.CoverageSuite")
        val dataManager = dataManagerClass.getMethod("getInstance", Project::class.java).invoke(null, project)
        val suite = dataManagerClass
            .getMethod("addExternalCoverageSuite", File::class.java, coverageRunnerClass)
            .invoke(dataManager, coverageFile, TolkCoverageRunner())
        dataManagerClass
            .getMethod("coverageGathered", coverageSuiteClass)
            .invoke(dataManager, suite)
    }

    companion object {
        const val RUNNER_ID: String = "TolkCoverageProgramRunner"
    }
}
