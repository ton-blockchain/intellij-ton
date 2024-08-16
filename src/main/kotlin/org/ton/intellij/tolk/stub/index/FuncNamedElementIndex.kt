package org.ton.intellij.tolk.stub.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.tolk.TolkFileElementType
import org.ton.intellij.tolk.psi.TolkNamedElement
import org.ton.intellij.util.checkCommitIsNotInProgress
import org.ton.intellij.util.getElements

class TolkNamedElementIndex : StringStubIndexExtension<TolkNamedElement>() {
    override fun getVersion(): Int = TolkFileElementType.stubVersion

    override fun getKey(): StubIndexKey<String, TolkNamedElement> = KEY

    companion object {
        val KEY: StubIndexKey<String, TolkNamedElement> =
            StubIndexKey.createIndexKey("org.ton.intellij.tolk.stub.index.TolkNamedElementIndex")

        fun findElementsByName(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<TolkNamedElement> {
            checkCommitIsNotInProgress(project)
            return getElements(KEY, target, project, scope).also {
//                println("try find $target = ${it.joinToString { it.text }}")
            }
        }
    }
}
