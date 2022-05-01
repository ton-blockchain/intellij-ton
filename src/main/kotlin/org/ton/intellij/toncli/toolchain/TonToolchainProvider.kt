package org.ton.intellij.toncli.toolchain

import com.intellij.openapi.extensions.ExtensionPointName
import java.nio.file.Path

interface TonToolchainProvider {
    fun getToolchain(homePath: Path): TonToolchainBase?

    companion object {
        private val EP_NAME: ExtensionPointName<TonToolchainProvider> =
                ExtensionPointName.create("org.ton.toolchainProvider")

        fun getToolchain(homePath: Path): TonToolchainBase? =
                EP_NAME.extensionList.asSequence()
                        .mapNotNull { it.getToolchain(homePath) }
                        .firstOrNull()
    }
}
