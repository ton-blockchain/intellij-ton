package org.ton.intellij.tolk.coverage

import com.intellij.coverage.CoverageDataManager
import com.intellij.coverage.CoverageExecutor
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.runners.executeState
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.guessProjectDir
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import java.io.File

class TolkCoverageProgramRunner : GenericProgramRunner<RunnerSettings>() {
    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return executorId == CoverageExecutor.EXECUTOR_ID && profile is ActonCommandConfiguration && profile.command == "test"
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        val executeState = executeState(state, environment, this)
        executeState?.processHandler?.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                val workingDir = environment.project.guessProjectDir()?.toNioPath()?.toFile() ?: return
                val dataManager = CoverageDataManager.getInstance(environment.project)
                val coverageFile = File(workingDir, "lcov.info")
                val suit = dataManager.addExternalCoverageSuite(coverageFile, TolkCoverageRunner())
                dataManager.coverageGathered(suit)
            }
        })
        return executeState
    }

    companion object {
        const val RUNNER_ID: String = "TolkCoverageProgramRunner"
    }
}
