package org.ton.intellij.tact.stub.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.tact.psi.TactFunction
import org.ton.intellij.tact.stub.TactFileStub
import org.ton.intellij.util.checkCommitIsNotInProgress
import org.ton.intellij.util.getElements

class TactFunctionIndex : StringStubIndexExtension<TactFunction>() {
    override fun getVersion(): Int = TactFileStub.Type.stubVersion

    override fun getKey(): StubIndexKey<String, TactFunction> = KEY

    companion object {
        val KEY =
            StubIndexKey.createIndexKey<String, TactFunction>("org.ton.intellij.tact.stub.index.TactFunctionIndex")

        fun findElementsByName(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<TactFunction> {
            checkCommitIsNotInProgress(project)
            return getElements(KEY, target, project, scope)
        }
    }
}
