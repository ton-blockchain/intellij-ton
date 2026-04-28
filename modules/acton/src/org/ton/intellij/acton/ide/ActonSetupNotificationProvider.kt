package org.ton.intellij.acton.ide

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationPanel.Status
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import org.ton.intellij.acton.ActonBundle
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.acton.cli.findActonExecutableInDefaultInstall
import org.ton.intellij.acton.settings.ActonConfigurable
import org.ton.intellij.acton.settings.actonSettings
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.Locale
import java.util.function.Function
import javax.swing.JComponent

class ActonSetupNotificationProvider(private val project: Project) : EditorNotificationProvider {
    override fun collectNotificationData(
        ignoredProject: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (file.name != "Acton.toml") return null
        ActonInstallSupport.syncConfiguredPath(project)
        if (ActonInstallSupport.isActonAvailable(project)) return null

        val platform = ActonInstallSupport.currentPlatform()

        return Function { fileEditor ->
            EditorNotificationPanel(fileEditor, Status.Warning).apply {
                text = ActonInstallSupport.missingActonMessage()
                ActonInstallSupport.configurePanelActions(this, project, platform)
            }
        }
    }
}

class ActonSetupNotificationStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (ActonToml.find(project) == null) return
        ActonInstallSupport.syncConfiguredPath(project)

        ApplicationManager.getApplication().invokeLater {
            if (!ActonInstallSupport.isActonAvailable(project)) {
                ActonInstallSupport.showMissingActonNotification(project)
            }
            EditorNotifications.getInstance(project).updateAllNotifications()
        }
    }
}

private object ActonInstallSupport {
    const val INSTALLATION_GUIDE_URL: String = "https://ton-blockchain.github.io/acton/docs/installation/"

    private const val INSTALL_COMMAND: String =
        "curl --proto '=https' --tlsv1.2 -LsSf https://github.com/i582/acton-public/releases/latest/download/acton-installer.sh | sh"
    private val DEFAULT_INSTALL_ROOT: Path = Path.of(System.getProperty("user.home"), ".acton")

    fun isActonAvailable(project: Project): Boolean {
        val configuredPath = project.actonSettings.actonPath?.trim().orEmpty()
        if (configuredPath.isNotEmpty() && isResolvableExecutable(configuredPath)) {
            return true
        }

        if (findActonExecutableInDefaultInstall() != null) {
            return true
        }

        return PathEnvironmentVariableUtil.findInPath("acton") != null
    }

    fun syncConfiguredPath(project: Project) {
        val defaultInstallPath = findActonExecutableInDefaultInstall() ?: return

        val configuredPath = project.actonSettings.actonPath?.trim().orEmpty()
        if (configuredPath == defaultInstallPath) return
        if (configuredPath.isNotEmpty() && isResolvableExecutable(configuredPath)) return

        project.actonSettings.actonPath = defaultInstallPath
    }

    fun currentPlatform(): Platform {
        val arch = normalizeArch(System.getProperty("os.arch"))

        return when {
            SystemInfo.isMac -> Platform(
                displayName = "macOS $arch",
                isSupported = arch == "ARM64" || arch == "x86_64",
            )

            SystemInfo.isLinux -> {
                val isSupportedArch = arch == "ARM64" || arch == "x86_64"
                val isSupported = isSupportedArch && isGnuLinux(arch)
                val osName = if (isSupported) "Linux GNU" else "Linux"
                Platform("$osName $arch", isSupported)
            }

            SystemInfo.isWindows -> Platform("Windows $arch", isSupported = false)
            else -> Platform(SystemInfo.OS_NAME + " " + arch, isSupported = false)
        }
    }

    fun runInstaller(project: Project) {
        var commandLine = GeneralCommandLine("/bin/sh", "-lc", INSTALL_COMMAND)
            .withWorkDirectory(project.guessProjectDir()?.path ?: System.getProperty("user.home"))

        commandLine = PtyCommandLine(commandLine)
            .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
            .withConsoleMode(true)

        val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
        val console = consoleBuilder.console
        val handler = KillableColoredProcessHandler(commandLine)
        handler.addProcessListener(object : ProcessListener {
            override fun processTerminated(event: ProcessEvent) {
                ApplicationManager.getApplication().invokeLater {
                    if (event.exitCode == 0) {
                        handleSuccessfulInstall(project)
                    }
                    EditorNotifications.getInstance(project).updateAllNotifications()
                }
            }
        })

        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val descriptor = RunContentDescriptor(
            console,
            handler,
            console.component,
            ActonBundle.message("notification.acton.toolchain.install.console.title"),
        )

        console.attachToProcess(handler)
        RunContentManager.getInstance(project).showRunContent(executor, descriptor)
        handler.startNotify()
    }

    fun missingActonMessage(): String = ActonBundle.message("notification.acton.toolchain.missing")

    fun configurePanelActions(
        panel: EditorNotificationPanel,
        project: Project,
        platform: Platform = currentPlatform(),
    ) {
        if (platform.isSupported) {
            panel.createActionLabel(ActonBundle.message("notification.acton.toolchain.action.install")) {
                runInstaller(project)
            }
        }

        panel.createActionLabel(ActonBundle.message("notification.acton.toolchain.action.configure")) {
            openSettings(project)
        }

        panel.createActionLabel(ActonBundle.message("notification.acton.toolchain.action.docs")) {
            BrowserUtil.browse(INSTALLATION_GUIDE_URL)
        }
    }

    fun showMissingActonNotification(project: Project) {
        val platform = currentPlatform()
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("Acton Setup")
            .createNotification(
                ActonBundle.message("notification.acton.toolchain.title"),
                missingActonMessage(),
                NotificationType.WARNING,
            )

        if (platform.isSupported) {
            notification.addAction(
                NotificationAction.createSimple(ActonBundle.message("notification.acton.toolchain.action.install")) {
                    notification.expire()
                    runInstaller(project)
                },
            )
        }

        notification.addAction(
            NotificationAction.createSimple(ActonBundle.message("notification.acton.toolchain.action.configure")) {
                notification.expire()
                openSettings(project)
            },
        )

        notification.addAction(
            NotificationAction.createSimple(ActonBundle.message("notification.acton.toolchain.action.docs")) {
                BrowserUtil.browse(INSTALLATION_GUIDE_URL)
            },
        )

        notification.notify(project)
    }

    private fun handleSuccessfulInstall(project: Project) {
        refreshInstallRelatedFiles(project)
        syncConfiguredPath(project)
        ActonToolchainSetupListener.EP_NAME.extensionList.forEach { it.actonInstalled(project) }
    }

    private fun refreshInstallRelatedFiles(project: Project) {
        LocalFileSystem.getInstance().refreshIoFiles(listOf(DEFAULT_INSTALL_ROOT.toFile()))
        project.guessProjectDir()?.let {
            VfsUtil.markDirtyAndRefresh(false, true, true, it)
        }
    }

    private fun openSettings(project: Project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, ActonConfigurable::class.java)
        EditorNotifications.getInstance(project).updateAllNotifications()
    }

    private fun isResolvableExecutable(pathOrCommand: String): Boolean {
        PathEnvironmentVariableUtil.findInPath(pathOrCommand)?.let { return true }

        return try {
            val path = Path.of(pathOrCommand)
            Files.isRegularFile(path) && Files.isExecutable(path)
        } catch (_: InvalidPathException) {
            false
        }
    }

    private fun isGnuLinux(arch: String): Boolean {
        val loaderCandidates = when (arch) {
            "x86_64" -> listOf(
                "/lib64/ld-linux-x86-64.so.2",
                "/lib/x86_64-linux-gnu/ld-linux-x86-64.so.2",
                "/usr/lib64/ld-linux-x86-64.so.2",
            )

            "ARM64" -> listOf(
                "/lib/ld-linux-aarch64.so.1",
                "/lib/aarch64-linux-gnu/ld-linux-aarch64.so.1",
                "/usr/lib/aarch64-linux-gnu/ld-linux-aarch64.so.1",
            )

            else -> return false
        }

        return loaderCandidates.any { Files.exists(Path.of(it)) }
    }

    private fun normalizeArch(rawArch: String?): String = when (rawArch?.lowercase(Locale.ROOT)) {
        "aarch64", "arm64" -> "ARM64"
        "x86_64", "amd64", "x64" -> "x86_64"
        null, "" -> "unknown"
        else -> rawArch
    }

    data class Platform(val displayName: String, val isSupported: Boolean)
}
