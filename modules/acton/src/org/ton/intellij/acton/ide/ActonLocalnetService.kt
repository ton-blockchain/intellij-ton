package org.ton.intellij.acton.ide

import com.google.gson.Gson
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.terminal.TerminalExecutionConsole
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.cli.ActonToml
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

const val ACTON_LOCALNET_TOOL_WINDOW_ID: String = "Acton Localnet"
private const val ACTON_LOCALNET_RUN_CONTENT_NAME: String = "Acton Localnet"
private const val DEFAULT_LOCALNET_PORT: Int = 5411
private const val DEFAULT_LOCALNET_AIRDROP_AMOUNT: Double = 100.0
private const val LOCALNET_STATUS_REFRESH_INTERVAL_MS: Long = 2_000
private const val DEFAULT_LOCALNET_STATE_PATH: String = "localnet-state.json"
private const val DEFAULT_LOCALNET_DB_PATH: String = "build/localnet.db"

val Project.actonLocalnetService: ActonLocalnetService
    get() = service()

@Service(Service.Level.PROJECT)
class ActonLocalnetService(private val project: Project) {
    private val lock = Any()
    private val gson = Gson()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()

    private var processHandler: KillableColoredProcessHandler? = null
    private var runContentDescriptor: RunContentDescriptor? = null
    private var status: LocalnetStatus = LocalnetStatus.STOPPED
    private var ownership: LocalnetOwnership = LocalnetOwnership.NONE
    private var port: Int = DEFAULT_LOCALNET_PORT
    private var managedTarget: LocalnetTarget? = null
    private var health: LocalnetHealth? = null
    private var lastError: String? = null
    private var healthRefreshInFlight: Boolean = false
    private var lastHealthRefreshStartedAt: Long = 0
    private var suppressNextManagedExitError: Boolean = false

    fun startFromProject() {
        val target = resolveTarget()
        if (target == null) {
            updateFailureState("Could not determine project directory for localnet")
            return
        }

        start(target)
    }

    fun start(targetDir: Path, resolvedPort: Int = DEFAULT_LOCALNET_PORT) {
        start(
            LocalnetTarget(
                workingDirectory = targetDir,
                settings = LocalnetResolvedSettings(port = resolvedPort),
            ),
        )
    }

    internal fun start(target: LocalnetTarget) {
        ApplicationManager.getApplication().executeOnPooledThread {
            restartProcess(target)
        }
    }

    fun stop() {
        val handler = synchronized(lock) {
            if (ownership != LocalnetOwnership.MANAGED) return
            val current = processHandler ?: return
            suppressNextManagedExitError = true
            status = LocalnetStatus.STOPPING
            current
        }

        handler.destroyProcess()
    }

    fun openUi() = BrowserUtil.browse(uiUrl(snapshot().port))

    fun restartFromProject() {
        val snapshot = snapshot()
        if (snapshot.isExternalActive()) {
            showError("Attached localnet is external. Restart it outside the IDE.")
            return
        }

        startFromProject()
    }

    fun promptAndAirdropFromProject() {
        val target = resolveTarget()
        if (target == null) {
            updateFailureState("Could not determine project directory for localnet")
            return
        }

        promptAndAirdrop(target)
    }

    fun promptAndAirdrop(targetDir: Path, resolvedPort: Int) {
        promptAndAirdrop(
            LocalnetTarget(
                workingDirectory = targetDir,
                settings = LocalnetResolvedSettings(port = resolvedPort),
            ),
        )
    }

    internal fun promptAndDumpStateFromProject() {
        val target = resolveTarget() ?: run {
            updateFailureState("Could not determine project directory for localnet")
            return
        }
        if (!snapshot().status.isActive()) return

        val path = promptForPath(
            title = "Acton Localnet Dump State",
            message = "Snapshot path",
            baseDir = target.workingDirectory,
            defaultPath = target.workingDirectory.resolve(DEFAULT_LOCALNET_STATE_PATH),
        ) ?: return

        dumpState(target, path)
    }

    internal fun promptAndLoadStateFromProject() {
        val target = resolveTarget() ?: run {
            updateFailureState("Could not determine project directory for localnet")
            return
        }
        if (!snapshot().status.isActive()) return

        val path = promptForPath(
            title = "Acton Localnet Load State",
            message = "Snapshot path",
            baseDir = target.workingDirectory,
            defaultPath = target.workingDirectory.resolve(DEFAULT_LOCALNET_STATE_PATH),
        ) ?: return

        loadState(target, path)
    }

    internal fun promptAndClearDbFromProject() {
        val target = resolveTarget() ?: run {
            updateFailureState("Could not determine project directory for localnet")
            return
        }
        val snapshot = snapshot()
        if (snapshot.isExternalActive()) {
            showError("External localnet is attached. Stop it outside the IDE before clearing its database.")
            return
        }

        val dbPath = promptForPath(
            title = "Acton Localnet Clear DB",
            message = "SQLite database path",
            baseDir = target.workingDirectory,
            defaultPath = target.workingDirectory.resolve(DEFAULT_LOCALNET_DB_PATH),
        ) ?: return

        val restartAfterClear = snapshot.isManagedActive()
        val message = if (restartAfterClear) {
            "Stop localnet, delete database files under\n$dbPath\nand start localnet again?"
        } else {
            "Delete database files under\n$dbPath\n?"
        }
        val confirmed = Messages.showYesNoDialog(
            project,
            message,
            "Acton Localnet",
            Messages.getQuestionIcon(),
        ) == Messages.YES
        if (!confirmed) return

        clearDb(target, dbPath, restartAfterClear)
    }

    fun showLog() {
        val state = synchronized(lock) {
            val handler = processHandler
            if (handler == null ||
                handler.isProcessTerminated ||
                ownership != LocalnetOwnership.MANAGED ||
                !status.isActive()
            ) {
                null
            } else {
                LocalnetRunContentState(handler, runContentDescriptor)
            }
        } ?: return

        ApplicationManager.getApplication().invokeLater {
            val executor = DefaultRunExecutor.getRunExecutorInstance()
            val runContentManager = RunContentManager.getInstance(project)
            val descriptor = state.descriptor
            if (descriptor != null && runContentManager.getAllDescriptors().contains(descriptor)) {
                runContentManager.toFrontRunContent(executor, descriptor)
            } else {
                val recreatedDescriptor = createRunContentDescriptor(state.handler)
                synchronized(lock) {
                    if (processHandler === state.handler && !state.handler.isProcessTerminated) {
                        runContentDescriptor = recreatedDescriptor
                    }
                }
                runContentManager.showRunContent(executor, recreatedDescriptor)
            }
        }
    }

    fun snapshot(): LocalnetSnapshot = synchronized(lock) {
        val managedAlive = processHandler?.let { !it.isProcessTerminated } == true
        if (ownership == LocalnetOwnership.MANAGED) {
            if (!managedAlive && status != LocalnetStatus.STARTING && status != LocalnetStatus.STOPPING) {
                status = LocalnetStatus.STOPPED
                ownership = LocalnetOwnership.NONE
                health = null
                managedTarget = null
            } else if (managedAlive && status == LocalnetStatus.STARTING) {
                status = LocalnetStatus.RUNNING
            }
        }
        if (!managedAlive && ownership != LocalnetOwnership.EXTERNAL) {
            healthRefreshInFlight = false
        }

        LocalnetSnapshot(
            status = status,
            ownership = ownership,
            port = port,
            lastBlockSeqno = health?.lastBlockSeqno,
            mode = health?.mode,
            forkSource = health?.forkSource,
            uptimeSeconds = health?.uptimeSeconds,
            lastError = lastError,
            hasRunContent = managedAlive && ownership == LocalnetOwnership.MANAGED && status.isActive(),
        )
    }

    fun requestHealthRefresh(force: Boolean = false) {
        val target = resolveTarget() ?: return
        val shouldRefresh = synchronized(lock) {
            val now = System.currentTimeMillis()
            if (healthRefreshInFlight ||
                (!force && now - lastHealthRefreshStartedAt < LOCALNET_STATUS_REFRESH_INTERVAL_MS)
            ) {
                false
            } else {
                healthRefreshInFlight = true
                lastHealthRefreshStartedAt = now
                port = target.port
                true
            }
        }
        if (!shouldRefresh) return

        ApplicationManager.getApplication().executeOnPooledThread {
            val statusResult = runLocalnetStatus(target)
            synchronized(lock) {
                healthRefreshInFlight = false

                val managedAlive = processHandler?.let { !it.isProcessTerminated } == true
                val isManagedTarget = managedTarget == target
                if (managedAlive && managedTarget != null && !isManagedTarget) {
                    return@executeOnPooledThread
                }

                port = target.port
                if (statusResult?.running == true) {
                    health = LocalnetHealth(
                        lastBlockSeqno = statusResult.lastBlockSeqno,
                        mode = statusResult.presentableMode(),
                        forkSource = statusResult.presentableForkSource(),
                        uptimeSeconds = statusResult.uptimeSeconds,
                    )
                    status = if (managedAlive) LocalnetStatus.RUNNING else LocalnetStatus.RUNNING
                    ownership = if (managedAlive) LocalnetOwnership.MANAGED else LocalnetOwnership.EXTERNAL
                    lastError = null
                } else {
                    if (managedAlive) {
                        health = null
                    } else {
                        health = null
                        status = LocalnetStatus.STOPPED
                        ownership = LocalnetOwnership.NONE
                    }
                }
            }
        }
    }

    private fun promptAndAirdrop(target: LocalnetTarget) {
        val address = Messages.showInputDialog(
            project,
            "Recipient address",
            "Acton Localnet Airdrop",
            null,
        )?.trim().orEmpty()
        if (address.isBlank()) return

        val amountText = Messages.showInputDialog(
            project,
            "Amount in TON",
            "Acton Localnet Airdrop",
            null,
            DEFAULT_LOCALNET_AIRDROP_AMOUNT.toString(),
            null,
        )?.trim().orEmpty()
        if (amountText.isBlank()) return

        val amountTon = amountText.toDoubleOrNull()
        if (amountTon == null || amountTon <= 0) {
            showError("Amount must be a positive number")
            return
        }

        airdrop(target, address, amountTon)
    }

    private fun promptForPath(title: String, message: String, baseDir: Path, defaultPath: Path): Path? {
        val text = Messages.showInputDialog(
            project,
            message,
            title,
            null,
            defaultPath.toString(),
            null,
        )?.trim().orEmpty()
        if (text.isBlank()) return null

        val candidate = Path.of(text)
        return if (candidate.isAbsolute) candidate else baseDir.resolve(candidate).normalize()
    }

    private fun restartProcess(target: LocalnetTarget) {
        val previousHandler = synchronized(lock) {
            processHandler?.takeIf { !it.isProcessTerminated }?.also {
                suppressNextManagedExitError = true
                status = LocalnetStatus.STOPPING
                it.destroyProcess()
            }
        }
        if (previousHandler != null) {
            previousHandler.waitFor(5_000)
        }

        val commandLine = ActonCommandLine(
            command = "localnet",
            workingDirectory = target.workingDirectory,
            additionalArguments = ActonCommand.Localnet.Start(
                port = target.settings.port,
                forkNet = target.settings.forkNet,
                forkBlockNumber = target.settings.forkBlockNumber,
                accounts = target.settings.accounts,
                rateLimit = target.settings.rateLimit,
            ).getArguments(),
            environmentVariables = EnvironmentVariablesData.DEFAULT,
        ).toGeneralCommandLine(project)

        if (commandLine == null) {
            updateFailureState("Cannot find acton executable")
            return
        }

        val ptyCommandLine = PtyCommandLine(commandLine)
            .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
            .withConsoleMode(false)

        val handler = try {
            KillableColoredProcessHandler(ptyCommandLine)
        } catch (e: Exception) {
            LOG.warn("Failed to create localnet process", e)
            updateFailureState(e.message ?: "Failed to create localnet process")
            return
        }
        val descriptor = createRunContentDescriptor(handler)

        synchronized(lock) {
            processHandler = handler
            runContentDescriptor = descriptor
            status = LocalnetStatus.STARTING
            ownership = LocalnetOwnership.MANAGED
            port = target.port
            managedTarget = target
            health = null
            lastError = null
            suppressNextManagedExitError = false
        }

        handler.addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: com.intellij.openapi.util.Key<*>) {
                synchronized(lock) {
                    if (status == LocalnetStatus.STARTING && !handler.isProcessTerminated) {
                        status = LocalnetStatus.RUNNING
                    }
                }
            }

            override fun processTerminated(event: ProcessEvent) {
                synchronized(lock) {
                    val shouldSuppressError = suppressNextManagedExitError
                    if (processHandler === handler) {
                        processHandler = null
                    }
                    if (runContentDescriptor?.processHandler === handler) {
                        runContentDescriptor = null
                    }
                    if (!shouldSuppressError && event.exitCode != 0) {
                        lastError = "Localnet exited with code ${event.exitCode}"
                    } else if (shouldSuppressError) {
                        lastError = null
                    }
                    suppressNextManagedExitError = false
                    health = null
                    status = LocalnetStatus.STOPPED
                    ownership = LocalnetOwnership.NONE
                    managedTarget = null
                }
            }
        })

        try {
            ApplicationManager.getApplication().invokeLater {
                RunContentManager.getInstance(project).showRunContent(
                    DefaultRunExecutor.getRunExecutorInstance(),
                    descriptor,
                )
            }
            handler.startNotify()
            requestHealthRefresh(force = true)
        } catch (e: Exception) {
            LOG.warn("Failed to start localnet process", e)
            updateFailureState(e.message ?: "Failed to start localnet process")
        }
    }

    private fun dumpState(target: LocalnetTarget, path: Path) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                path.parent?.let(Files::createDirectories)
            } catch (e: Exception) {
                LOG.warn("Failed to prepare state dump path", e)
                showError("Failed to prepare state dump path: ${e.message}")
                return@executeOnPooledThread
            }

            val error = invokeAdminPathAction(target, "dump-state", path)
            ApplicationManager.getApplication().invokeLater {
                if (error == null) {
                    requestHealthRefresh(force = true)
                    Messages.showInfoMessage(project, "State dumped to $path", "Acton Localnet")
                } else {
                    Messages.showErrorDialog(project, error, "Acton Localnet")
                }
            }
        }
    }

    private fun loadState(target: LocalnetTarget, path: Path) {
        ApplicationManager.getApplication().executeOnPooledThread {
            if (!Files.exists(path)) {
                showError("State snapshot does not exist: $path")
                return@executeOnPooledThread
            }

            val error = invokeAdminPathAction(target, "load-state", path)
            ApplicationManager.getApplication().invokeLater {
                if (error == null) {
                    requestHealthRefresh(force = true)
                    Messages.showInfoMessage(project, "State loaded from $path", "Acton Localnet")
                } else {
                    Messages.showErrorDialog(project, error, "Acton Localnet")
                }
            }
        }
    }

    private fun clearDb(target: LocalnetTarget, dbPath: Path, restartAfterClear: Boolean) {
        ApplicationManager.getApplication().executeOnPooledThread {
            if (restartAfterClear) {
                val handler = synchronized(lock) {
                    val current = processHandler
                    if (ownership != LocalnetOwnership.MANAGED || current == null) {
                        null
                    } else {
                        suppressNextManagedExitError = true
                        status = LocalnetStatus.STOPPING
                        current
                    }
                }
                handler?.destroyProcess()
                handler?.waitFor(5_000)
            }

            val deletedPaths = try {
                deleteDatabaseFiles(dbPath)
            } catch (e: Exception) {
                LOG.warn("Failed to clear localnet database", e)
                showError("Failed to clear database: ${e.message}")
                return@executeOnPooledThread
            }

            if (deletedPaths.isNotEmpty()) {
                LocalFileSystem.getInstance().refreshIoFiles(deletedPaths.map(Path::toFile))
            }

            ApplicationManager.getApplication().invokeLater {
                val message = if (deletedPaths.isEmpty()) {
                    "Database files were not found under $dbPath"
                } else {
                    "Deleted ${deletedPaths.size} database file(s)"
                }
                Messages.showInfoMessage(project, message, "Acton Localnet")
            }

            if (restartAfterClear) {
                start(target)
            } else {
                requestHealthRefresh(force = true)
            }
        }
    }

    private fun deleteDatabaseFiles(dbPath: Path): List<Path> {
        val fileName = dbPath.fileName?.toString().orEmpty()
        val walPath = dbPath.resolveSibling("$fileName-wal")
        val shmPath = dbPath.resolveSibling("$fileName-shm")
        val candidates = listOf(dbPath, walPath, shmPath)
        val deleted = mutableListOf<Path>()
        candidates.forEach { path ->
            if (Files.exists(path)) {
                Files.delete(path)
                deleted.add(path)
            }
        }
        return deleted
    }

    private fun invokeAdminPathAction(target: LocalnetTarget, action: String, path: Path): String? {
        val requestBody = gson.toJson(LocalnetStatePathPayload(path.toString()))
        var lastErrorMessage: String? = null
        for (host in LOCALNET_HOSTS) {
            val url = URI.create("http://$host:${target.port}/admin/$action")
            val request = HttpRequest.newBuilder(url)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val result = runCatching {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            }
            val response = result.getOrNull()
            if (response == null) {
                lastErrorMessage =
                    "Failed to call localnet admin API on port ${target.port}: ${result.exceptionOrNull()?.message}"
                continue
            }
            if (response.statusCode() !in 200..299) {
                return "Localnet admin API returned ${response.statusCode()}: ${response.body()}"
            }

            val payload = runCatching {
                gson.fromJson(response.body(), LocalnetAdminResponse::class.java)
            }.getOrElse {
                return "Failed to parse localnet admin response: ${it.message}"
            }

            if (!payload.ok) {
                return payload.error ?: "Localnet admin request failed"
            }
            return null
        }
        return lastErrorMessage ?: "Failed to call localnet admin API on port ${target.port}"
    }

    private fun resolveTarget(): LocalnetTarget? = ReadAction.compute<LocalnetTarget?, RuntimeException> {
        val activeManagedTarget = synchronized(lock) {
            if (ownership == LocalnetOwnership.MANAGED || status == LocalnetStatus.STOPPING) managedTarget else null
        }
        if (activeManagedTarget != null) {
            return@compute activeManagedTarget
        }

        val actonToml = ActonToml.find(project)
        if (actonToml != null) {
            return@compute actonToml.toLocalnetTarget(DEFAULT_LOCALNET_PORT)
        }

        project.guessProjectDir()?.toNioPath()?.let {
            LocalnetTarget(
                workingDirectory = it,
                settings = LocalnetResolvedSettings(port = DEFAULT_LOCALNET_PORT),
            )
        }
    }

    private fun airdrop(target: LocalnetTarget, address: String, amountTon: Double) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val commandLine = ActonCommandLine(
                command = "localnet",
                workingDirectory = target.workingDirectory,
                additionalArguments = ActonCommand.Localnet.Airdrop(
                    address = address,
                    amountTon = amountTon,
                    port = target.port,
                ).getArguments(),
                environmentVariables = EnvironmentVariablesData.DEFAULT,
            ).toGeneralCommandLine(project)

            if (commandLine == null) {
                showError("Cannot find acton executable")
                return@executeOnPooledThread
            }

            val output = CapturingProcessHandler(commandLine).runProcess(30_000)
            ApplicationManager.getApplication().invokeLater {
                if (output.exitCode == 0) {
                    val message = output.stdout.trim().ifBlank { "Airdrop completed" }
                    requestHealthRefresh(force = true)
                    Messages.showInfoMessage(project, message, "Acton Localnet")
                } else {
                    val message = output.stderr.trim().ifBlank { output.stdout.trim() }.ifBlank {
                        "Localnet airdrop failed"
                    }
                    Messages.showErrorDialog(project, message, "Acton Localnet")
                }
            }
        }
    }

    private fun runLocalnetStatus(target: LocalnetTarget): LocalnetStatusCommandResult? {
        val commandLine = ActonCommandLine(
            command = "localnet",
            workingDirectory = target.workingDirectory,
            additionalArguments = ActonCommand.Localnet.Status(
                port = target.port,
                json = true,
            ).getArguments(),
            environmentVariables = EnvironmentVariablesData.DEFAULT,
        ).toGeneralCommandLine(project) ?: return null

        return try {
            val output = CapturingProcessHandler(commandLine).runProcess(10_000)
            if (output.exitCode != 0) {
                LOG.warn("Failed to query localnet status: ${output.stderr.ifBlank { output.stdout }}")
                null
            } else {
                gson.fromJson(output.stdout, LocalnetStatusCommandResult::class.java)
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse localnet status", e)
            null
        }
    }

    private fun updateFailureState(message: String) {
        synchronized(lock) {
            processHandler = null
            runContentDescriptor = null
            status = LocalnetStatus.STOPPED
            ownership = LocalnetOwnership.NONE
            health = null
            managedTarget = null
            lastError = message
            suppressNextManagedExitError = false
        }

        showError(message)
    }

    private fun showError(message: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(project, message, "Acton Localnet")
        }
    }

    private fun createRunContentDescriptor(handler: KillableColoredProcessHandler): RunContentDescriptor {
        val console = TerminalExecutionConsole(project, null)
        console.attachToProcess(handler)
        return RunContentDescriptor(
            console,
            handler,
            console.component,
            ACTON_LOCALNET_RUN_CONTENT_NAME,
        )
    }

    companion object {
        private val LOG = logger<ActonLocalnetService>()
        private val LOCALNET_HOSTS = listOf("127.0.0.1", "localhost")

        fun uiUrl(port: Int): String = "http://localhost:$port/"

        fun explorerAddressUrl(port: Int, address: String): String {
            val encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8.toString()).replace("+", "%20")
            return "${uiUrl(port)}explorer/address/$encodedAddress"
        }
    }
}

private data class LocalnetStatePathPayload(val path: String)

private data class LocalnetAdminResponse(val ok: Boolean, val error: String?)
