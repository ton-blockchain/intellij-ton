package org.ton.intellij.tolk.stub.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import org.ton.intellij.tolk.TolkFileElementType
import org.ton.intellij.tolk.psi.TolkTypeSymbolElement

class TolkTypeSymbolIndex : StringStubIndexExtension<TolkTypeSymbolElement>() {
    override fun getVersion(): Int = TolkFileElementType.stubVersion

    override fun getKey(): StubIndexKey<String, TolkTypeSymbolElement> = KEY

    companion object {
        val KEY: StubIndexKey<String, TolkTypeSymbolElement> =
            StubIndexKey.createIndexKey("org.ton.intellij.tolk.stub.index.TolkTypeSymbolIndex")

        fun findElements(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<TolkTypeSymbolElement> {
            return StubIndex.getElements(KEY, target, project, scope, TolkTypeSymbolElement::class.java)
        }

        fun processElements(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
            processor: (TolkTypeSymbolElement) -> Unit
        ) {
            StubIndex.getInstance().processElements(
                KEY,
                target,
                project,
                scope,
                TolkTypeSymbolElement::class.java
            ) { element ->
                processor(element)
                true
            }
        }

        fun processAllElements(
            project: Project,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
            processor: (TolkTypeSymbolElement) -> Unit
        ) {
            StubIndex.getInstance().processAllKeys(KEY, Processor { key ->
                processElements(project, key, scope, processor)
                true
            }, scope)
        }
    }
}
