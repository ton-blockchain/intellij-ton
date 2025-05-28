package org.ton.intellij.tolk.stub.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import org.ton.intellij.tolk.TolkFileElementType
import org.ton.intellij.tolk.psi.TolkNamedElement

class TolkNamedElementIndex : StringStubIndexExtension<TolkNamedElement>() {
    override fun getVersion(): Int = TolkFileElementType.stubVersion

    override fun getKey(): StubIndexKey<String, TolkNamedElement> = KEY

    companion object {
        val KEY: StubIndexKey<String, TolkNamedElement> =
            StubIndexKey.createIndexKey("org.ton.intellij.tolk.stub.index.TolkNamedElementIndex")

        fun processAllElements(
            project: Project,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
            processor: Processor<in TolkNamedElement>
        ): Boolean = processAllKeys(project, scope) {
            processElements(project, it, scope, processor)
        }

        fun processElements(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
            processor: Processor<in TolkNamedElement>
        ): Boolean = StubIndex.getInstance().processElements(
            KEY,
            target,
            project,
            scope,
            TolkNamedElement::class.java,
            processor
        )

        fun processAllKeys(
            project: Project,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
            processor: Processor<String>
        ): Boolean = StubIndex.getInstance().processAllKeys(KEY, processor, scope)
    }
}
