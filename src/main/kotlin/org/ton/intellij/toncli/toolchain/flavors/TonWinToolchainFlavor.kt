/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.ton.intellij.toncli.toolchain.flavors

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import org.ton.intellij.list
import org.ton.intellij.toPath
import java.nio.file.Path

class TonWinToolchainFlavor : TonToolchainFlavor() {

    override fun getHomePathCandidates(): Sequence<Path> {
        val programFiles = System.getenv("ProgramFiles")?.toPath() ?: return emptySequence()
        if (!programFiles.exists() || !programFiles.isDirectory()) return emptySequence()
        return programFiles.list()
                .filter { it.isDirectory() }
                .filter {
                    val name = FileUtil.getNameWithoutExtension(it.fileName.toString())
                    name.lowercase().startsWith("toncli")
                }
                .map { it.resolve("bin") }
                .filter { it.isDirectory() }
    }

    override fun isApplicable(): Boolean = SystemInfo.isWindows
}
