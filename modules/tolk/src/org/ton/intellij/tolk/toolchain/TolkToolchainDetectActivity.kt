package org.ton.intellij.tolk.toolchain

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.ton.intellij.tolk.ide.configurable.tolkSettings

class TolkToolchainDetectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        LOG.warn("Detecting Tolk standard library for project ${project.name}")
        val stdlibDir = project.tolkSettings.stdlibDir
        LOG.warn("Detected Tolk standard library: $stdlibDir")
    }

    companion object {
        private val LOG = logger<TolkToolchainDetectActivity>()
    }
}
