package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkNamedElement
import java.nio.file.Path

object TolkTestLocator : SMTestLocator {
    override fun getLocation(
        protocol: String,
        path: String,
        project: Project,
        scope: GlobalSearchScope,
    ): MutableList<Location<PsiElement>> {
        if (protocol != PROTOCOL_ID) {
            return mutableListOf()
        }

        val (filepath, functionName) = path.split(":")
        val file = VirtualFileManager.getInstance().findFileByNioPath(Path.of(filepath)) ?: return mutableListOf()
        val psiFile = PsiManager.getInstance(project).findFile(file) as? TolkFile ?: return mutableListOf()

        val function = psiFile.functions.find { it.name == functionName } ?: return mutableListOf()

        return mutableListOf(PsiLocation(function))
    }

    fun getTestUrl(element: TolkNamedElement): String {
        val containingFile = element.containingFile
        val name = element.name ?: return ""
        return "$PROTOCOL_ID://${containingFile.virtualFile.path}:$name"
    }

    private const val PROTOCOL_ID = "tolk_qn"
}
