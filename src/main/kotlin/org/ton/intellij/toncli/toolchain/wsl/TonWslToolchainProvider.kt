package org.ton.intellij.toncli.toolchain.wsl

import com.intellij.execution.wsl.WslPath
import org.ton.intellij.ide.features.TonFeatures
import org.ton.intellij.isFeatureEnabled
import org.ton.intellij.toncli.toolchain.TonToolchainBase
import org.ton.intellij.toncli.toolchain.TonToolchainProvider
import java.nio.file.Path

class TonWslToolchainProvider : TonToolchainProvider {
    override fun getToolchain(homePath: Path): TonToolchainBase? {
        if (!isFeatureEnabled(TonFeatures.WSL_TOOLCHAIN)) return null
        val wslPath = WslPath.parseWindowsUncPath(homePath.toString()) ?: return null
        return TonWslToolchain(wslPath)
    }
}