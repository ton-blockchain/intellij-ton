package org.ton.intellij.acton.ide

import com.google.gson.annotations.SerializedName
import org.ton.intellij.acton.cli.ActonToml
import java.nio.file.Path

enum class LocalnetStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    ;

    fun presentableName(): String = when (this) {
        STOPPED -> "Stopped"
        STARTING -> "Starting"
        RUNNING -> "Running"
        STOPPING -> "Stopping"
    }

    fun isActive(): Boolean = this == STARTING || this == RUNNING
}

enum class LocalnetOwnership {
    NONE,
    MANAGED,
    EXTERNAL,
    ;

    fun presentableName(): String = when (this) {
        NONE -> "Not attached"
        MANAGED -> "Managed by IDE"
        EXTERNAL -> "Attached to external localnet"
    }
}

data class LocalnetSnapshot(
    val status: LocalnetStatus,
    val ownership: LocalnetOwnership,
    val port: Int,
    val lastBlockSeqno: Long?,
    val mode: String?,
    val forkSource: String?,
    val uptimeSeconds: Long?,
    val lastError: String?,
    val hasRunContent: Boolean,
)

internal data class LocalnetHealth(
    val lastBlockSeqno: Long?,
    val mode: String?,
    val forkSource: String?,
    val uptimeSeconds: Long?,
)

internal data class LocalnetRunContentState(
    val handler: com.intellij.execution.process.KillableColoredProcessHandler,
    val descriptor: com.intellij.execution.ui.RunContentDescriptor?,
)

internal data class LocalnetResolvedSettings(
    val port: Int = 5411,
    val forkNet: String? = null,
    val forkBlockNumber: Long? = null,
    val accounts: List<String> = emptyList(),
    val rateLimit: Int? = null,
)

internal data class LocalnetTarget(val workingDirectory: Path, val settings: LocalnetResolvedSettings) {
    val port: Int get() = settings.port
}

internal fun LocalnetSnapshot.isManagedActive(): Boolean = ownership == LocalnetOwnership.MANAGED && status.isActive()

internal fun LocalnetSnapshot.isExternalActive(): Boolean = ownership == LocalnetOwnership.EXTERNAL && status.isActive()

internal fun ActonToml.toLocalnetTarget(defaultPort: Int = 5411): LocalnetTarget = LocalnetTarget(
    workingDirectory = workingDir,
    settings = getLocalnetSettings().toResolvedLocalnetSettings(defaultPort),
)

internal fun ActonToml.LocalnetSettings?.toResolvedLocalnetSettings(defaultPort: Int = 5411): LocalnetResolvedSettings =
    LocalnetResolvedSettings(
        port = this?.port ?: defaultPort,
        forkNet = this?.forkNet,
        forkBlockNumber = this?.forkBlockNumber,
        accounts = this?.accounts.orEmpty(),
        rateLimit = this?.rateLimit,
    )

internal fun ActonToml.LocalnetSettings?.presentableMode(): String = presentableMode(
    stateSource = if (this?.forkNet.isNullOrBlank()) "local" else "remote",
    forkNetwork = this?.forkNet,
    forkBlockNumber = this?.forkBlockNumber,
)

internal fun ActonToml.LocalnetSettings?.presentableForkSource(): String? = presentableForkSource(
    stateSource = if (this?.forkNet.isNullOrBlank()) "local" else "remote",
    forkNetwork = this?.forkNet,
    forkBlockNumber = this?.forkBlockNumber,
)

private fun presentableMode(stateSource: String?, forkNetwork: String?, forkBlockNumber: Long?): String = when {
    stateSource == "remote" && !forkNetwork.isNullOrBlank() && forkBlockNumber != null ->
        "Forked from $forkNetwork at seqno $forkBlockNumber"
    stateSource == "remote" && !forkNetwork.isNullOrBlank() -> "Forked from $forkNetwork"
    stateSource == "remote" -> "Forked"
    stateSource == "local" -> "Local genesis"
    stateSource.isNullOrBlank() -> "Unknown"
    else -> stateSource
}

private fun presentableForkSource(stateSource: String?, forkNetwork: String?, forkBlockNumber: Long?): String? = when {
    stateSource == "remote" && !forkNetwork.isNullOrBlank() && forkBlockNumber != null ->
        "$forkNetwork at seqno $forkBlockNumber"
    stateSource == "remote" && !forkNetwork.isNullOrBlank() -> forkNetwork
    else -> null
}

internal data class LocalnetStatusCommandResult(
    val running: Boolean,
    @SerializedName("uptime_seconds")
    val uptimeSeconds: Long?,
    @SerializedName("last_block_seqno")
    val lastBlockSeqno: Long?,
    @SerializedName("state_source")
    val stateSource: String?,
    @SerializedName("fork_network")
    val forkNetwork: String?,
    @SerializedName("fork_block_number")
    val forkBlockNumber: Long?,
) {
    fun presentableMode(): String = presentableMode(stateSource, forkNetwork, forkBlockNumber)

    fun presentableForkSource(): String? = presentableForkSource(stateSource, forkNetwork, forkBlockNumber)
}
