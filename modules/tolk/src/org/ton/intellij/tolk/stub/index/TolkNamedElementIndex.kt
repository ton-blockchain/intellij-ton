package org.ton.intellij.tolk.stub.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.tolk.TolkFileElementType
import org.ton.intellij.tolk.psi.TolkNamedElement
import org.ton.intellij.util.checkCommitIsNotInProgress

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
            val elements = StubIndex.getElements(
                KEY, target, project, scope, TolkNamedElement::class.java
            )
            return elements
        }
    }
}
