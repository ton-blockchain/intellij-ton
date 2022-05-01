package org.ton.intellij.toncli.toolchain.wsl

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WSLUtil
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.io.isDirectory
import org.ton.intellij.computeWithCancelableProgress
import org.ton.intellij.ide.features.TonFeatures
import org.ton.intellij.isDispatchThread
import org.ton.intellij.isFeatureEnabled
import org.ton.intellij.resolveOrNull
import org.ton.intellij.toncli.toolchain.flavors.TonToolchainFlavor
import java.nio.file.Path

class TonWslToolchainFlavor : TonToolchainFlavor() {
    override fun getHomePathCandidates(): Sequence<Path> = sequence {
        val distributions = compute("Getting installed distributions...") {
            WslDistributionManager.getInstance().installedDistributions
        }
        for (distro in distributions) {
            yieldAll(distro.getHomePathCandidates())
        }
    }

    override fun isApplicable(): Boolean =
            WSLUtil.isSystemCompatible() && isFeatureEnabled(TonFeatures.WSL_TOOLCHAIN)

    override fun isValidToolchainPath(path: Path): Boolean =
            WslDistributionManager.isWslPath(path.toString()) && super.isValidToolchainPath(path)

    override fun hasExecutable(path: Path, toolName: String): Boolean = path.hasExecutableOnWsl(toolName)

    override fun pathToExecutable(path: Path, toolName: String): Path = path.pathToExecutableOnWsl(toolName)
}

fun WSLDistribution.getHomePathCandidates(): Sequence<Path> = sequence {
    @Suppress("UnstableApiUsage")
    val root = uncRootPath
    val environment = compute("Getting environment variables...") { environment }
    if (environment != null) {
        val home = environment["HOME"]
        val remoteCargoPath = home?.let { "$it/.cargo/bin" }
        val localCargoPath = remoteCargoPath?.let { root.resolve(it) }
        if (localCargoPath?.isDirectory() == true) {
            yield(localCargoPath)
        }

        val sysPath = environment["PATH"]
        for (remotePath in sysPath.orEmpty().split(":")) {
            if (remotePath.isEmpty()) continue
            val localPath = root.resolveOrNull(remotePath) ?: continue
            if (!localPath.isDirectory()) continue
            yield(localPath)
        }
    }

    for (remotePath in listOf("/usr/local/bin", "/usr/bin")) {
        val localPath = root.resolve(remotePath)
        if (!localPath.isDirectory()) continue
        yield(localPath)
    }
}

private fun <T> compute(
        @Suppress("UnstableApiUsage") @NlsContexts.ProgressTitle title: String,
        getter: () -> T
): T = if (isDispatchThread) {
    val project = ProjectManager.getInstance().defaultProject
    project.computeWithCancelableProgress(title, getter)
} else {
    getter()
}
