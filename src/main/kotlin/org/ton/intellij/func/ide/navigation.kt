package org.ton.intellij.func.ide

import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.GotoClassContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.func.psi.FuncElement
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.stub.FuncFileIndex

abstract class FuncNavigationContributorBase<T>(
    private val indexKey: StubIndexKey<String, T>,
    private val clazz: Class<T>
) : ChooseByNameContributor, GotoClassContributor where T : NavigationItem, T : FuncElement {
    override fun getNames(project: Project?, includeNonProjectItems: Boolean): Array<String> = when (project) {
        null -> emptyArray()
        else -> StubIndex.getInstance().getAllKeys(indexKey, project).toTypedArray()
    }

    override fun getItemsByName(
        name: String?,
        pattern: String?,
        project: Project?,
        includeNonProjectItems: Boolean
    ): Array<NavigationItem> {
        if (name == null || project == null) return emptyArray()
        val scope = when {
            includeNonProjectItems -> GlobalSearchScope.allScope(project)
            else -> GlobalSearchScope.projectScope(project)
        }
        return StubIndex.getElements(indexKey, name, project, scope, clazz).toTypedArray()
    }

    override fun getQualifiedName(item: NavigationItem?): String? = item?.name

    override fun getQualifiedNameSeparator(): String? = "."
}

class FuncFileNavigationContributor : FuncNavigationContributorBase<FuncFile>(FuncFileIndex.KEY, FuncFile::class.java)