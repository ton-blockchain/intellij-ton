package org.ton.intellij.tact.stub.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.ton.intellij.tact.psi.TactTypeDeclarationElement
import org.ton.intellij.tact.stub.TactFileStub
import org.ton.intellij.util.checkCommitIsNotInProgress
import org.ton.intellij.util.getElements

class TactTypesIndex : StringStubIndexExtension<TactTypeDeclarationElement>() {
    override fun getVersion(): Int = TactFileStub.Type.stubVersion

    override fun getKey(): StubIndexKey<String, TactTypeDeclarationElement> = KEY

    companion object {
        val KEY =
            StubIndexKey.createIndexKey<String, TactTypeDeclarationElement>("org.ton.intellij.tact.stub.index.TactTypesIndex")

        fun findElementsByName(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<TactTypeDeclarationElement> {
            checkCommitIsNotInProgress(project)
            return getElements(KEY, target, project, scope)
        }
    }
}
