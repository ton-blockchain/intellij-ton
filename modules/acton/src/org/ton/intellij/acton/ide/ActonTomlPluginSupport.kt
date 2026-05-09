package org.ton.intellij.acton.ide

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import org.jetbrains.annotations.TestOnly

private val TOML_PLUGIN_ID: PluginId = PluginId.getId("org.toml.lang")

@Volatile
private var tomlPluginInstalledOverride: Boolean? = null

fun isTomlPluginInstalled(): Boolean =
    tomlPluginInstalledOverride ?: (PluginManagerCore.getPlugin(TOML_PLUGIN_ID)?.isEnabled == true)

fun hasActonToml(project: Project): Boolean {
    val contentRoots = ProjectRootManager.getInstance(project).contentRoots
    for (root in contentRoots) {
        if (containsActonToml(root)) return true
    }

    val projectDir = project.guessProjectDir()
    return projectDir != null && projectDir !in contentRoots && containsActonToml(projectDir)
}

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

    return ProjectRootManager.getInstance(project).fileIndex.getContentRootForFile(startDir)
}

private fun containsActonToml(root: VirtualFile): Boolean {
    if (!root.isDirectory) return root.name == "Acton.toml"

    var found = false
    VfsUtilCore.visitChildrenRecursively(
        root,
        object : VirtualFileVisitor<Unit>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (file.name == "Acton.toml" && !file.isDirectory) {
                    found = true
                    return false
                }
                return !found
            }
        },
    )
    return found
}

@TestOnly
fun setTomlPluginInstalledOverride(value: Boolean?) {
    tomlPluginInstalledOverride = value
}
