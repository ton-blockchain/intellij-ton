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

        fun processElements(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
            processor: (TolkNamedElement) -> Unit
        ) {
            StubIndex.getInstance().processElements(
                KEY,
                target,
                project,
                scope,
                TolkNamedElement::class.java
            ) { element ->
                processor(element)
                true
            }
        }

        fun processAllElements(
            project: Project,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
            processor: (TolkNamedElement) -> Unit
        ) {
            StubIndex.getInstance().processAllKeys(KEY, Processor { key ->
                processElements(project, key, scope, processor)
                true
            }, scope)
        }
    }
}
