package org.ton.intellij.func.stub.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.func.FuncFileElementType
import org.ton.intellij.func.psi.FuncNamedElement
import org.ton.intellij.util.checkCommitIsNotInProgress
import org.ton.intellij.util.getElements

class FuncNamedElementIndex : StringStubIndexExtension<FuncNamedElement>() {
    override fun getVersion(): Int = FuncFileElementType.stubVersion

    override fun getKey(): StubIndexKey<String, FuncNamedElement> = KEY

    companion object {
        val KEY: StubIndexKey<String, FuncNamedElement> =
            StubIndexKey.createIndexKey("org.ton.intellij.func.stub.index.FuncNamedElementIndex")

        fun findElementsByName(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<FuncNamedElement> {
            checkCommitIsNotInProgress(project)
            return getElements(KEY, target, project, scope).also {
//                println("try find $target = ${it.joinToString { it.text }}")
            }
        }
    }
}
