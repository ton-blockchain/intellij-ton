package org.ton.intellij.tolk.stub.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.tolk.TolkFileElementType
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.util.checkCommitIsNotInProgress
import org.ton.intellij.util.getElements

class TolkTypeDefIndex : StringStubIndexExtension<TolkTypeDef>() {
    override fun getVersion(): Int = TolkFileElementType.stubVersion

    override fun getKey(): StubIndexKey<String, TolkTypeDef> = KEY

    companion object {
        val KEY: StubIndexKey<String, TolkTypeDef> =
            StubIndexKey.createIndexKey("org.ton.intellij.tolk.stub.index.TolkTypeDefIndex")

        fun findElementsByName(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<TolkTypeDef> {
            checkCommitIsNotInProgress(project)
            return getElements(KEY, target, project, scope).also {
            }
        }
    }
}
