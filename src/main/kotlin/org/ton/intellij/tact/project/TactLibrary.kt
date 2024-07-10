package org.ton.intellij.tact.project

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDirectory
import com.intellij.openapi.vfs.findFile
import org.ton.intellij.tact.TactIcons
import javax.swing.Icon

class TactLibrary(
    private val name: String,
    private val sourceRoots: Set<VirtualFile>,
    private val version: String? = null
) : SyntheticLibrary(), ItemPresentation {
    override fun equals(other: Any?): Boolean = other is TactLibrary && other.sourceRoots == sourceRoots

    override fun hashCode(): Int = sourceRoots.hashCode()

    override fun getPresentableText(): String = if (version != null) "$name $version" else name

    override fun getIcon(unused: Boolean): Icon = TactIcons.FILE

    override fun getSourceRoots(): Collection<VirtualFile> = sourceRoots
}

class TactAdditionalLibraryRootsProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
        val stdlibLibrary = makeStdlibLibrary(project) ?: return emptyList()
        return listOf(stdlibLibrary)
    }
}

private fun makeStdlibLibrary(project: Project): TactLibrary? {
    val sourceRoots = mutableSetOf<VirtualFile>()

    val projectFile = project.getBaseDirectories().firstOrNull() ?: return null
    val stdlibSrc = projectFile.findDirectory("node_modules/@tact-lang/compiler/stdlib")
    if (stdlibSrc != null) {
        sourceRoots.addAll(stdlibSrc.children)
        val packageJson = stdlibSrc.parent.findFile("package.json")
        if (packageJson != null) {
            val version = packageJson.inputStream.bufferedReader().use { reader ->
                val regex = """"version":\s*"([^"]+)"""".toRegex()
                for (line in reader.readText().lines()) {
                    val match = regex.find(line) ?: continue
                    return@use match.groupValues[1]
                }
                null
            }
            return TactLibrary("stdlib", sourceRoots, version)
        }
        return TactLibrary("stdlib", sourceRoots)
    }
    return null
}
