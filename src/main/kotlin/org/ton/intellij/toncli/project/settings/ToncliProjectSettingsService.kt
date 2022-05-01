package org.ton.intellij.toncli.project.settings

import com.intellij.util.io.systemIndependentPath
import com.intellij.util.xmlb.annotations.Transient
import org.ton.intellij.toncli.toolchain.TonToolchainBase
import org.ton.intellij.toncli.toolchain.TonToolchainProvider
import java.nio.file.Paths

interface ToncliProjectSettingsService {
    val toolchain: TonToolchainBase?

    data class State(
            var version: Int? = null,
            var toolchainHomeDirectory: String? = null,
    ) {
        @get:Transient
        @set:Transient
        var toolchain: TonToolchainBase?
            get() = toolchainHomeDirectory?.let { TonToolchainProvider.getToolchain(Paths.get(it)) }
            set(value) {
                toolchainHomeDirectory = value?.location?.systemIndependentPath
            }
    }

    /**
     * Allows to modify settings.
     * After setting change,
     */
    fun modify(action: (State) -> Unit)
}