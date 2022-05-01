package org.ton.intellij.toncli.toolchain.tools

import org.ton.intellij.execute
import org.ton.intellij.toncli.toolchain.TonToolchainBase
import org.ton.intellij.toncli.tools.TonTool
import java.nio.file.Path

fun TonToolchainBase.toncli() = Toncli(this)

class Toncli(
        toolchain: TonToolchainBase
) : TonTool("toncli", toolchain) {
    fun queryVersion(workingDirectory: Path? = null): String? {
        val lines = createBaseCommandLine("--version", workingDirectory = workingDirectory)
                .execute(toolchain.executionTimeoutInMilliseconds)
                ?.stdoutLines
        return lines?.firstOrNull()?.substring(7)
    }

    companion object {
        const val NAME = "toncli"
    }
}