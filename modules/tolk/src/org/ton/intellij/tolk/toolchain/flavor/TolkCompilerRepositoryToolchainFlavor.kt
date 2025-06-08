package org.ton.intellij.tolk.toolchain.flavor

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.toNioPathOrNull
import java.nio.file.Path
import kotlin.io.path.isDirectory

class TolkCompilerRepositoryToolchainFlavor : TolkToolchainFlavor() {
    override fun getHomePathCandidates(project: Project): Sequence<Path> {
        val projectDir = project.guessProjectDir() ?: return emptySequence()
        return sequenceOf(projectDir.toNioPathOrNull())
            .filterNotNull()
            .filter { isValidToolchainPath(it) }
    }

    override fun isValidToolchainPath(path: Path): Boolean {
        return super.isValidToolchainPath(path) && path.resolve("tolk").isDirectory()
    }
}
