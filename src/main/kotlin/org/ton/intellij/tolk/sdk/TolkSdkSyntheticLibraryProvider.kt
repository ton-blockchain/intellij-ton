package org.ton.intellij.tolk.sdk

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import org.ton.intellij.tact.TactIcons
import org.ton.intellij.tact.project.TactLibrary
import org.ton.intellij.tolk.TolkIcons
import javax.swing.Icon

class TolkSdkSyntheticLibraryProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): MutableCollection<SyntheticLibrary> {
        val tolkSdkManager = TolkSdkManager[project]
        val tolkSdk = tolkSdkManager.getSdkRef().resolve(project)
        return mutableListOf(tolkSdk?.library ?: return mutableListOf())
    }
}

class TolkLibrary(
    private val name: String,
    private val sourceRoots: Set<VirtualFile>,
    private val version: String? = null
) : SyntheticLibrary(), ItemPresentation {
    override fun equals(other: Any?): Boolean = other is TactLibrary && other.sourceRoots == sourceRoots

    override fun hashCode(): Int = sourceRoots.hashCode()

    override fun getPresentableText(): String = if (version != null) "$name $version" else name

    override fun getIcon(unused: Boolean): Icon = TolkIcons.FILE

    override fun getSourceRoots(): Collection<VirtualFile> = sourceRoots
}
