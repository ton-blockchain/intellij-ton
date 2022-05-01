package org.ton.intellij.toncli.project.model

import com.intellij.ide.plugins.PluginManagerCore.isUnitTestMode
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.VirtualFile
import org.ton.intellij.ide.notifications.showBalloon
import org.ton.intellij.pathAsPath
import org.ton.intellij.toncli.project.settings.ToncliProjectSettingsService
import org.ton.intellij.toncli.toolchain.TonToolchainBase
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

interface ToncliProjectService {
    val project: Project
    val allProjects: Collection<ToncliProject>
    val hasAtLeastOneValidProject: Boolean
    val initialized: Boolean

    fun attachToncliProject(manifest: Path): Boolean
    fun attachToncliProjects(vararg manifests: Path)
    fun detachToncliProject(cargoProject: ToncliProject)
    fun refreshAllProjects(): CompletableFuture<out List<ToncliProject>>
    fun discoverAndRefresh(): CompletableFuture<out List<ToncliProject>>
    fun suggestManifests(): Sequence<VirtualFile>
}

interface ToncliProject : UserDataHolderEx {
    val project: Project
    val manifest: Path
    val rootDir: VirtualFile?
    val workspaceRootDir: VirtualFile?
}

fun guessAndSetupToncliProject(project: Project, explicitRequest: Boolean = false): Boolean {
    if (!explicitRequest) {
        val alreadyTried = run {
            val key = "org.ton.intellij.toncli.project.model.PROJECT_DISCOVERY"
            val properties = PropertiesComponent.getInstance(project)
            val alreadyTried = properties.getBoolean(key)
            properties.setValue(key, true)
            alreadyTried
        }
        if (alreadyTried) return false
    }

    val toolchain = project.toncliSettings.toolchain
    if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
        discoverToolchain(project)
        return true
    }
    if (!project.toncliProhjects.hasAtLeastOneValidProject) {
        project.toncliProhjects.discoverAndRefresh()
        return true
    }
    return false
}

private fun discoverToolchain(project: Project) {
    val projectPath = project.guessProjectDir()?.pathAsPath
    val toolchain = TonToolchainBase.suggest(projectPath) ?: return
    invokeLater {
        if (project.isDisposed) return@invokeLater

        val oldToolchain = project.toncliSettings.toolchain
        if (oldToolchain != null && oldToolchain.looksLikeValidToolchain()) {
            return@invokeLater
        }

        runWriteAction {
            project.toncliSettings.modify { it.toolchain = toolchain }
        }

        val tool = "toncli at ${toolchain.presentableLocation}"
        project.showBalloon("Using $tool", NotificationType.INFORMATION)
        project.toncliProhjects.discoverAndRefresh()
    }
}

val Project.toncliProhjects: ToncliProjectService get() = service()

val Project.toncliSettings: ToncliProjectSettingsService
    get() = getService(ToncliProjectSettingsService::class.java)
            ?: error("Failed to get ToncliProjectSettingsService for $this")

val Project.tonToolchain: TonToolchainBase?
    get() {
        val toolchain = toncliSettings.toolchain
        return when {
            toolchain != null -> toolchain
            isUnitTestMode -> TonToolchainBase.suggest()
            else -> null
        }
    }