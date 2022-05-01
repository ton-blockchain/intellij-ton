package org.ton.intellij.toncli.toolchain.flavors

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.util.io.isDirectory
import org.ton.intellij.toncli.toolchain.tools.Toncli
import org.ton.intellij.toncli.util.hasExecutable
import org.ton.intellij.toncli.util.pathToExecutable
import java.nio.file.Path

abstract class TonToolchainFlavor {
    fun suggestHomePaths(): Sequence<Path> = getHomePathCandidates().filter { isValidToolchainPath(it) }

    protected abstract fun getHomePathCandidates(): Sequence<Path>

    /**
     * Checks if the path is the name of a Rust toolchain of this flavor.
     *
     * @param path path to check.
     * @return true if paths points to a valid home.
     */
    protected open fun isValidToolchainPath(path: Path): Boolean {
        return path.isDirectory() &&
                hasExecutable(path, Toncli.NAME)
    }

    protected open fun hasExecutable(path: Path, toolName: String): Boolean = path.hasExecutable(toolName)

    /**
     * Flavor is added to result in [getApplicableFlavors] if this method returns true.
     * @return whether this flavor is applicable.
     */
    open fun isApplicable(): Boolean = true

    open fun pathToExecutable(path: Path, toolName: String): Path = path.pathToExecutable(toolName)

    companion object {
        private val EP_NAME: ExtensionPointName<TonToolchainFlavor> =
                ExtensionPointName.create("org.ton.toolchainFlavor")

        fun getApplicableFlavors(): List<TonToolchainFlavor> =
                EP_NAME.extensionList.filter { it.isApplicable() }

        fun getFlavor(path: Path): TonToolchainFlavor? =
                getApplicableFlavors().find { flavor -> flavor.isValidToolchainPath(path) }
    }
}