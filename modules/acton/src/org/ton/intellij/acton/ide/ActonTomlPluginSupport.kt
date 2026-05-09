package org.ton.intellij.acton.ide

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.TestOnly

private val TOML_PLUGIN_ID: PluginId = PluginId.getId("org.toml.lang")

@Volatile
private var tomlPluginInstalledOverride: Boolean? = null

fun isTomlPluginInstalled(): Boolean =
    tomlPluginInstalledOverride ?: (PluginManagerCore.getPlugin(TOML_PLUGIN_ID)?.isEnabled == true)

fun hasActonToml(project: Project): Boolean = project.guessProjectDir()?.findChild("Acton.toml")?.isDirectory == false

fun hasNearestActonToml(project: Project, from: VirtualFile): Boolean = findNearestActonToml(project, from) != null

private fun findNearestActonToml(project: Project, from: VirtualFile): VirtualFile? {
    val startDir = if (from.isDirectory) from else from.parent ?: return null
    val stopDir = findSearchRoot(project, startDir) ?: return null

    var dir: VirtualFile? = startDir
    while (dir != null) {
        dir.findChild("Acton.toml")?.takeUnless { it.isDirectory }?.let { return it }
        if (dir == stopDir) break
        dir = dir.parent
    }
    return null
}

private fun findSearchRoot(project: Project, startDir: VirtualFile): VirtualFile? {
    val projectDir = project.guessProjectDir()
    if (projectDir != null && (projectDir == startDir || VfsUtilCore.isAncestor(projectDir, startDir, false))) {
        return projectDir
    }

    return com.intellij.openapi.roots.ProjectRootManager.getInstance(project).fileIndex.getContentRootForFile(startDir)
}

@TestOnly
fun setTomlPluginInstalledOverride(value: Boolean?) {
    tomlPluginInstalledOverride = value
}
