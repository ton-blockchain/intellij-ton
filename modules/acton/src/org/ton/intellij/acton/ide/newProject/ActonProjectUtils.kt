package org.ton.intellij.acton.ide.newProject

import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandConfigurationType
import java.nio.file.Paths

fun createDefaultRunConfigurations(project: Project, baseDir: VirtualFile) {
    val runManager = RunManager.getInstance(project)
    val factory = ActonCommandConfigurationType.getInstance().factory

    // Build all contracts
    val buildSettings = runManager.createConfiguration("Build all contracts", factory)
    val buildConfig = buildSettings.configuration as ActonCommandConfiguration
    buildConfig.command = "build"
    buildConfig.workingDirectory = Paths.get(baseDir.path)
    runManager.addConfiguration(buildSettings)

    // Run all tests
    val testSettings = runManager.createConfiguration("Run all tests", factory)
    val testConfig = testSettings.configuration as ActonCommandConfiguration
    testConfig.command = "test"
    testConfig.testMode = ActonCommand.Test.TestMode.DIRECTORY
    testConfig.testTarget = "."
    testConfig.workingDirectory = Paths.get(baseDir.path)
    runManager.addConfiguration(testSettings)

    runManager.selectedConfiguration = buildSettings
}
