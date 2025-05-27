package org.ton.intellij.tolk.toolchain

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.toolchain.flavor.TolkToolchainFlavor
import java.nio.file.Path
import kotlin.io.path.pathString

class TolkToolchainDetectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        LOG.warn("Detecting toolchain for project ${project.name}")

        val knownToolchains = TolkKnownToolchains.knownToolchains
        val tolkToolchainService = project.tolkSettings

        val needFindToolchains = knownToolchains.isEmpty()
        if (needFindToolchains) {
            LOG.warn("Known toolchains are empty, searching for toolchains")
            val toolchainCandidates = collectToolchainCandidates(project)
            if (toolchainCandidates.isEmpty()) {
                return
            }
            LOG.warn("Found toolchain candidates: $toolchainCandidates")
            tolkToolchainService.toolchain = TolkToolchain.fromPath(toolchainCandidates.first())
            LOG.warn("Selected toolchain: ${tolkToolchainService.toolchain}")
            return
        }

        if (tolkToolchainService.toolchain == TolkToolchain.NULL) {
            val suggested = TolkToolchain.suggest(project)
            if (suggested != null) {
                LOG.warn("Suggested toolchain: $suggested")
                tolkToolchainService.toolchain = suggested
                return
            }

            if (knownToolchains.isNotEmpty()) {
                LOG.warn("No toolchain selected, using the first known toolchain")
                tolkToolchainService.toolchain = TolkToolchain.fromPath(knownToolchains.first())
                LOG.warn("Selected toolchain: ${tolkToolchainService.toolchain}")
            }
            return
        }

        LOG.warn("Detected toolchain: ${tolkToolchainService.toolchain}")
    }

    companion object {
        private val LOG = logger<TolkToolchainDetectActivity>()

        private fun collectToolchainCandidates(project: Project): Set<Path> {
            val toolchainCandidates = TolkToolchainFlavor.getApplicableFlavors()
                .flatMap { it.suggestHomePaths(project) }
                .toSet()

            if (toolchainCandidates.isEmpty()) {
                return emptySet()
            }

            TolkKnownToolchainsState.knownToolchains = toolchainCandidates.map { it.pathString }.toSet()
            return toolchainCandidates
        }
    }
}
