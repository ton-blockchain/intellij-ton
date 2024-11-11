package org.ton.intellij.tolk.sdk

import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile

class TolkSdk(
    val stdlibFile: VirtualFile,
) {
    val version: String? by lazy {
        val packageJson = stdlibFile.parent.parent.findFile("package.json")
        packageJson?.inputStream?.bufferedReader()?.use { reader ->
            val regex = """"version":\s*"([^"]+)"""".toRegex()
            for (line in reader.readText().lines()) {
                val match = regex.find(line) ?: continue
                return@use match.groupValues[1]
            }
            null
        }
    }
    val library: SyntheticLibrary by lazy {
        library()
    }

    private fun library(): SyntheticLibrary {
        return TolkLibrary("Tolk", setOf(stdlibFile), version)
    }
}
