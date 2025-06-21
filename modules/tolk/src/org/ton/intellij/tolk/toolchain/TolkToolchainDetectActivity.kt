package org.ton.intellij.tolk.toolchain

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.ton.intellij.tolk.ide.configurable.tolkSettings

class TolkToolchainDetectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        LOG.warn("Detecting toolchain for project ${project.name}")

        val tolkToolchainService = project.tolkSettings
        guessAndSetupTolkProject(project, true)

        LOG.warn("Detected toolchain: ${tolkToolchainService.toolchain}")
    }

    companion object {
        private val LOG = logger<TolkToolchainDetectActivity>()
    }
}
