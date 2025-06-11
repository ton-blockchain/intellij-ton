package org.ton.intellij.tolk.toolchain

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.diagnostic.logger
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

        val stdlibDir = settings.stdlibDir
        if (stdlibDir == null) {
//            LOG.warn("Tolk stdlib dir is null, toolchain: ${settings.toolchain}")
            return emptyList()
        }

        val toolchain = settings.toolchain
        if (toolchain.stdlibDir == stdlibDir) {
            val library = TolkLibrary(toolchain)
//            LOG.warn("Found Tolk stdlib: $stdlibDir")
            return listOf(library)
        }

        val library = TolkLibrary("Tolk stdlib", stdlibDir)
//        LOG.warn("Found unversioned Tolk stdlib: $stdlibDir")
        return listOf(library)
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

    companion object {
        private val LOG = logger<TolkSdkSyntheticLibraryProvider>()
    }
}
