package org.ton.intellij.tolk.stub.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import org.ton.intellij.tolk.TolkFileElementType
import org.ton.intellij.tolk.psi.TolkFunction

class TolkFunctionIndex : StringStubIndexExtension<TolkFunction>() {
    override fun getVersion(): Int = TolkFileElementType.stubVersion

    override fun getKey(): StubIndexKey<String, TolkFunction> = KEY

    companion object {
        val KEY: StubIndexKey<String, TolkFunction> =
            StubIndexKey.createIndexKey("org.ton.intellij.tolk.stub.index.TolkFunctionIndex")

        fun findElements(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<TolkFunction> {
           return StubIndex.getElements(KEY, target, project, scope, TolkFunction::class.java)
        }

        fun processAllElements(
            project: Project,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
            processor: (TolkFunction) -> Unit
        ) {
            StubIndex.getInstance().processAllKeys(KEY, Processor { key ->
                StubIndex.getInstance().processElements(
                    KEY,
                    key,
                    project,
                    scope,
                    TolkFunction::class.java
                ) { element ->
                    processor(element)
                    true
                }
                true
            }, scope)
        }
    }
}
