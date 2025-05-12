package org.ton.intellij.tolk.toolchain.flavor

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import java.nio.file.Path
import kotlin.io.path.isDirectory

abstract class TolkToolchainFlavor {
    fun suggestHomePaths(project: Project): Sequence<Path> =
        getHomePathCandidates(project).filter { isValidToolchainPath(it) }

    protected abstract fun getHomePathCandidates(project: Project): Sequence<Path>

    protected open fun isApplicable(): Boolean = true

    protected open fun isValidToolchainPath(path: Path): Boolean {
        return path.isDirectory()
    }

    companion object {
        private val EP_NAME = ExtensionPointName.create<TolkToolchainFlavor>("org.ton.tolk.toolchainFlavor")

        fun getApplicableFlavors(): List<TolkToolchainFlavor> = EP_NAME.extensionList.filter { it.isApplicable() }

        fun getFlavor(path: Path): TolkToolchainFlavor? =
            getApplicableFlavors().find { flavor -> flavor.isValidToolchainPath(path) }
    }
}
