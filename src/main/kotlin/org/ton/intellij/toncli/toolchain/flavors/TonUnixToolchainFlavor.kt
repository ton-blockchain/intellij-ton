package org.ton.intellij.toncli.toolchain.flavors

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.isDirectory
import java.nio.file.Path

class TonUnixToolchainFlavor : TonToolchainFlavor() {
    override fun getHomePathCandidates(): Sequence<Path> =
            sequenceOf("/usr/local/bin", "/usr/bin")
                    .map { it.toPath() }
                    .filter { it.isDirectory() }

    override fun isApplicable(): Boolean = SystemInfo.isUnix
}