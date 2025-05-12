package org.ton.intellij.tolk.toolchain

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import javax.swing.Icon

class TolkSdkSyntheticLibraryProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
        val settings = project.tolkSettings

        val stdlibDir = settings.stdlibDir ?: return emptyList()

        val toolchain = settings.toolchain
        if (toolchain != null && toolchain.stdlibDir == stdlibDir) {
            return listOf(TolkLibrary(toolchain))
        }

        return listOf(TolkLibrary("Tolk stdlib", stdlibDir))
    }

    override fun getRootsToWatch(project: Project) =
        getAdditionalProjectLibraries(project).flatMap { it.sourceRoots }

    data class TolkLibrary(
        private val name: String,
        private val sourceRoot: VirtualFile?,
    ) : SyntheticLibrary(), ItemPresentation {
        constructor(toolchain: TolkToolchain) : this("Tolk ${toolchain.version}", toolchain.stdlibDir)

        override fun getSourceRoots() =
            if (sourceRoot == null) emptyList() else listOf(sourceRoot)

        override fun getPresentableText(): String = name

        override fun getIcon(unused: Boolean): Icon = TolkIcons.FILE
    }
}
