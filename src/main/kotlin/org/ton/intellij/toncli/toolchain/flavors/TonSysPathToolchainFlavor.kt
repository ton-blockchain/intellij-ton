package org.ton.intellij.toncli.toolchain.flavors

import com.intellij.util.io.isDirectory
import org.ton.intellij.toPathOrNull
import java.io.File
import java.nio.file.Path

class TonSysPathToolchainFlavor : TonToolchainFlavor() {
    override fun getHomePathCandidates(): Sequence<Path> =
            System.getenv("PATH")
                    .orEmpty()
                    .split(File.pathSeparator)
                    .asSequence()
                    .filter { it.isNotEmpty() }
                    .mapNotNull { it.toPathOrNull() }
                    .filter { it.isDirectory() }
}