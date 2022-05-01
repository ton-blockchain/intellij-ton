package org.ton.intellij.toncli.toolchain

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.wsl.WslPath
import com.intellij.util.io.exists
import org.ton.intellij.toncli.toolchain.flavors.TonToolchainFlavor
import org.ton.intellij.toncli.toolchain.tools.Toncli
import org.ton.intellij.toncli.toolchain.wsl.getHomePathCandidates
import java.nio.file.Path

abstract class TonToolchainBase(
        val location: Path
) {
    val presentableLocation: String get() = pathToExecutable(Toncli.NAME).toString()
    abstract val fileSeparator: String
    abstract val executionTimeoutInMilliseconds: Int

    fun looksLikeValidToolchain(): Boolean = TonToolchainFlavor.getFlavor(location) != null

    /**
     * Patches passed command line to make it runnable in remote context.
     */
    abstract fun patchCommandLine(commandLine: GeneralCommandLine): GeneralCommandLine

    abstract fun toLocalPath(remotePath: String): String

    abstract fun toRemotePath(localPath: String): String

    abstract fun expandUserHome(remotePath: String): String

    protected abstract fun getExecutableName(toolName: String): String

    // for executables from toolchain
    abstract fun pathToExecutable(toolName: String): Path

    fun pathToToncliExecutable(toolName: String): Path {
        val exePath = pathToExecutable(toolName)
        if (exePath.exists()) return exePath
        else throw IllegalStateException("Installed toncli required: https://github.com/disintar/toncli/blob/master/INSTALLATION.md")
        // TODO: toncli binaries: https://github.com/disintar/toncli/issues/36
//        val toncliBin = expandUserHome("~/.toncli/bin")
//        val exeName = getExecutableName(toolName)
//        return Paths.get(toncliBin, exeName)
    }

    abstract fun hasExecutable(exec: String): Boolean

    abstract fun hasToncliExecutable(exec: String): Boolean

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TonToolchainBase) return false

        if (location != other.location) return false

        return true
    }

    override fun hashCode(): Int = location.hashCode()

    companion object {
        fun suggest(projectDir: Path? = null): TonToolchainBase? {
            val distribution = projectDir?.let { WslPath.getDistributionByWindowsUncPath(it.toString()) }
            val toolchain = distribution
                    ?.getHomePathCandidates()
                    ?.filter { TonToolchainFlavor.getFlavor(it) != null }
                    ?.mapNotNull { TonToolchainProvider.getToolchain(it.toAbsolutePath()) }
                    ?.firstOrNull()
            if (toolchain != null) return toolchain

            return TonToolchainFlavor.getApplicableFlavors()
                    .asSequence()
                    .flatMap { it.suggestHomePaths() }
                    .mapNotNull { TonToolchainProvider.getToolchain(it.toAbsolutePath()) }
                    .firstOrNull()
        }
    }
}