package org.ton.intellij.tolk.acton

import com.intellij.openapi.project.Project
import org.ton.intellij.acton.ide.ActonToolchainSetupListener
import org.ton.intellij.tolk.ide.configurable.tolkSettings

class TolkActonToolchainSetupListener : ActonToolchainSetupListener {
    override fun actonInstalled(project: Project) {
        project.tolkSettings.refreshDetectedStdlib()
    }
}
