package org.ton.intellij.tolk.sdk

import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.findDirectory
import java.io.File

class TolkSdkRef(
    val referenceName: String
) {
    fun resolve(project: Project): TolkSdk? {
        if (referenceName.isEmpty()) {
            project.getBaseDirectories().forEach { baseDir ->
                val nodeModules = baseDir.findDirectory("node_modules") ?: return@forEach
                val tonModule = nodeModules.findChild("@ton") ?: return@forEach
                if (!tonModule.isDirectory) {
                    return@forEach
                }
                val tolkModule = tonModule.findChild("tolk-js") ?: return@forEach
                val distDir = tolkModule.findChild("dist") ?: return@forEach
                if (!distDir.isDirectory) {
                    return@forEach
                }
                val stdlib = distDir.findChild("stdlib.tolk") ?: return@forEach
                return TolkSdk(stdlib)
            }
        } else {
            val file = VfsUtil.findFileByIoFile(File(referenceName), true)
            if (file != null) {
                return TolkSdk(file)
            }
        }
        return null
    }

    override fun toString(): String = referenceName
}
