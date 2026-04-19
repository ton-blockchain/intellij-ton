package org.ton.intellij.acton.ide.newProject

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger

internal data class ActonTemplateCatalog(
    @SerializedName("schema_version")
    val schemaVersion: Int,
    val templates: List<ActonTemplateDefinition>,
) {
    fun templateIds(): List<String> = templates.map { it.id }

    fun normalizedDefaultTemplate(): String = when {
        templates.any { it.id == ActonProjectSettings.DEFAULT_TEMPLATE } -> ActonProjectSettings.DEFAULT_TEMPLATE
        templates.isNotEmpty() -> templates.first().id
        else -> ActonProjectSettings.DEFAULT_TEMPLATE
    }

    fun supportsTypeScriptApp(templateId: String): Boolean = templates
        .firstOrNull { it.id == templateId }
        ?.supportsApp == true

    fun starterFilePath(templateId: String, includeTypeScriptApp: Boolean): String? = templates
        .firstOrNull { it.id == templateId }
        ?.selectScaffold(includeTypeScriptApp)
        ?.contracts
        ?.firstOrNull()
        ?.src
}

internal data class ActonTemplateDefinition(
    val id: String,
    val description: String,
    @SerializedName("supports_app")
    val supportsApp: Boolean,
    val scaffolds: List<ActonTemplateScaffold>,
) {
    fun selectScaffold(includeTypeScriptApp: Boolean): ActonTemplateScaffold? {
        val preferredScaffold = if (includeTypeScriptApp) {
            scaffolds.firstOrNull { it.includesTypeScriptApp }
        } else {
            scaffolds.firstOrNull { !it.includesTypeScriptApp }
        }
        return preferredScaffold ?: scaffolds.firstOrNull()
    }
}

internal data class ActonTemplateScaffold(
    val kind: String,
    @SerializedName("includes_typescript_app")
    val includesTypeScriptApp: Boolean,
    val contracts: List<ActonTemplateContract>,
)

internal data class ActonTemplateContract(val id: String, val name: String, val src: String)

internal object ActonTemplateCatalogProvider {
    private val gson = Gson()

    @Volatile
    private var cachedCatalog: ActonTemplateCatalog? = null

    @Volatile
    private var refreshScheduled: Boolean = false

    fun getTemplateCatalog(): ActonTemplateCatalog = getTemplateCatalog(
        isDispatchThread = ApplicationManager.getApplication()?.isDispatchThread == true,
        loader = ::loadTemplateCatalog,
        scheduleBackgroundRefresh = { refresh ->
            ApplicationManager.getApplication()?.executeOnPooledThread(refresh) ?: refresh.run()
        },
    )

    internal fun getTemplateCatalog(
        isDispatchThread: Boolean,
        loader: () -> ActonTemplateCatalog,
        scheduleBackgroundRefresh: (Runnable) -> Unit,
    ): ActonTemplateCatalog {
        cachedCatalog?.let { return it }
        if (isDispatchThread) {
            scheduleBackgroundRefreshIfNeeded(loader, scheduleBackgroundRefresh)
            return fallbackCatalog()
        }
        return synchronized(this) {
            cachedCatalog ?: loader().also { cachedCatalog = it }
        }
    }

    private fun scheduleBackgroundRefreshIfNeeded(
        loader: () -> ActonTemplateCatalog,
        scheduleBackgroundRefresh: (Runnable) -> Unit,
    ) {
        if (cachedCatalog != null || refreshScheduled) return

        val shouldSchedule = synchronized(this) {
            if (cachedCatalog != null || refreshScheduled) {
                false
            } else {
                refreshScheduled = true
                true
            }
        }
        if (!shouldSchedule) return

        scheduleBackgroundRefresh(
            Runnable {
                try {
                    val catalog = cachedCatalog ?: loader()
                    synchronized(this) {
                        if (cachedCatalog == null) {
                            cachedCatalog = catalog
                        }
                    }
                } finally {
                    refreshScheduled = false
                }
            },
        )
    }

    internal fun loadTemplateCatalog(
        commandRunner: (List<String>) -> ActonTemplatesCommandOutput = ::runTemplatesCommand,
    ): ActonTemplateCatalog = try {
        val output = commandRunner(listOf("new", "--templates"))
        if (output.exitCode != 0) {
            LOG.warn("Failed to load Acton templates, exit code=${output.exitCode}")
            fallbackCatalog()
        } else {
            parseTemplateCatalog(output.stdout) ?: fallbackCatalog().also {
                LOG.warn("Failed to parse Acton template catalog, using fallback")
            }
        }
    } catch (e: Exception) {
        LOG.warn("Failed to load Acton template catalog, using fallback", e)
        fallbackCatalog()
    }

    internal fun parseTemplateCatalog(stdout: String): ActonTemplateCatalog? = runCatching {
        gson.fromJson(stdout, ActonTemplateCatalog::class.java)
    }.getOrNull()
        ?.takeIf { it.templates.isNotEmpty() }

    private fun runTemplatesCommand(args: List<String>): ActonTemplatesCommandOutput {
        val actonPath = PathEnvironmentVariableUtil.findInPath("acton")?.absolutePath ?: "acton"
        val output = CapturingProcessHandler(
            GeneralCommandLine(actonPath).withParameters(args),
        ).runProcess(5000)
        return ActonTemplatesCommandOutput(
            exitCode = output.exitCode,
            stdout = output.stdout,
        )
    }

    private fun fallbackCatalog(): ActonTemplateCatalog = FALLBACK_TEMPLATE_CATALOG

    private val FALLBACK_TEMPLATE_CATALOG = ActonTemplateCatalog(
        schemaVersion = 1,
        templates = listOf(
            ActonTemplateDefinition(
                id = "empty",
                description = "Minimal project skeleton",
                supportsApp = false,
                scaffolds = listOf(
                    ActonTemplateScaffold(
                        kind = "standard",
                        includesTypeScriptApp = false,
                        contracts = listOf(
                            ActonTemplateContract(
                                id = "Empty",
                                name = "Empty",
                                src = "contracts/Empty.tolk",
                            ),
                        ),
                    ),
                ),
            ),
            ActonTemplateDefinition(
                id = "counter",
                description = "Simple counter contract",
                supportsApp = true,
                scaffolds = listOf(
                    ActonTemplateScaffold(
                        kind = "standard",
                        includesTypeScriptApp = false,
                        contracts = listOf(
                            ActonTemplateContract(
                                id = "Counter",
                                name = "Counter",
                                src = "contracts/Counter.tolk",
                            ),
                        ),
                    ),
                    ActonTemplateScaffold(
                        kind = "app",
                        includesTypeScriptApp = true,
                        contracts = listOf(
                            ActonTemplateContract(
                                id = "Counter",
                                name = "Counter",
                                src = "contracts/src/Counter.tolk",
                            ),
                        ),
                    ),
                ),
            ),
            ActonTemplateDefinition(
                id = "jetton",
                description = "Jetton minter and wallet contracts",
                supportsApp = false,
                scaffolds = listOf(
                    ActonTemplateScaffold(
                        kind = "standard",
                        includesTypeScriptApp = false,
                        contracts = listOf(
                            ActonTemplateContract(
                                id = "JettonMinter",
                                name = "JettonMinter",
                                src = "contracts/JettonMinter.tolk",
                            ),
                            ActonTemplateContract(
                                id = "JettonWallet",
                                name = "JettonWallet",
                                src = "contracts/JettonWallet.tolk",
                            ),
                        ),
                    ),
                ),
            ),
            ActonTemplateDefinition(
                id = "nft",
                description = "NFT collection and item contracts",
                supportsApp = false,
                scaffolds = listOf(
                    ActonTemplateScaffold(
                        kind = "standard",
                        includesTypeScriptApp = false,
                        contracts = listOf(
                            ActonTemplateContract(
                                id = "NftCollection",
                                name = "NftCollection",
                                src = "contracts/NftCollection.tolk",
                            ),
                            ActonTemplateContract(
                                id = "NftItem",
                                name = "NftItem",
                                src = "contracts/NftItem.tolk",
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    internal data class ActonTemplatesCommandOutput(val exitCode: Int, val stdout: String)

    private val LOG = logger<ActonTemplateCatalogProvider>()
}
