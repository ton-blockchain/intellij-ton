package org.ton.intellij.acton.ide

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface ActonToolchainSetupListener {
    fun actonInstalled(project: Project)

    companion object {
        val EP_NAME: ExtensionPointName<ActonToolchainSetupListener> = ExtensionPointName.create(
            "org.ton.intellij.acton.toolchainSetupListener",
        )
    }
}
