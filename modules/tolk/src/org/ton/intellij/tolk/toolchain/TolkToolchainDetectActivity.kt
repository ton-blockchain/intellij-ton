package org.ton.intellij.tolk.toolchain

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.toolchain.flavor.TolkToolchainFlavor
import java.nio.file.Path
import kotlin.io.path.pathString

class TolkToolchainDetectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val knownToolchains = TolkKnownToolchains.knownToolchains
        val tolkToolchainService = project.tolkSettings

        val needFindToolchains = knownToolchains.isEmpty()
        if (needFindToolchains) {
            val toolchainCandidates = collectToolchainCandidates(project)
            if (toolchainCandidates.isEmpty()) {
                return
            }
            tolkToolchainService.toolchain = TolkToolchain.fromPath(toolchainCandidates.first())
            return
        }

        if (tolkToolchainService.toolchain == TolkToolchain.NULL && knownToolchains.isNotEmpty()) {
            tolkToolchainService.toolchain = TolkToolchain.fromPath(knownToolchains.first())
        }
    }

    companion object {
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
