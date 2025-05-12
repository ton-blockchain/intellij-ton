package org.ton.intellij.tolk.toolchain.flavor

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.toNioPathOrNull
import java.nio.file.Path
import kotlin.io.path.isDirectory

class TolkNodeModulesToolchainFlavor : TolkToolchainFlavor() {
    override fun getHomePathCandidates(project: Project): Sequence<Path> {
        val projectDir = project.guessProjectDir() ?: return emptySequence()
        return sequenceOf("${projectDir.path}/node_modules/@ton/tolk-js".toNioPathOrNull())
            .filterNotNull()
            .filter { it.isDirectory() }
    }
}
