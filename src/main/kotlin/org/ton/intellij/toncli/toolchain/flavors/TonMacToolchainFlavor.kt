package org.ton.intellij.toncli.toolchain.flavors

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.isDirectory
import org.ton.intellij.toPath
import java.nio.file.Path

class TonMacToolchainFlavor : TonToolchainFlavor() {

    override fun getHomePathCandidates(): Sequence<Path> {
        val path = "/usr/local/Cellar/toncli/bin".toPath()
        return if (path.isDirectory()) {
            sequenceOf(path)
        } else {
            emptySequence()
        }
    }

    override fun isApplicable(): Boolean = SystemInfo.isMac
}
