package org.ton.intellij.func.psi

import com.intellij.openapi.project.Project
import org.ton.intellij.loadTextResource

class FuncInternalFactory(project: Project) {
    val psiFactory = FuncPsiFactory(project)

    val stdLib by lazy {
        val text = loadTextResource(FuncInternalFactory::class, "func/stdlib.fc")
        // TODO: read only file
        psiFactory.createFile(text, "stdlib", true).also {
            it.virtualFile.isWritable = false
        }
    }

    companion object {
        operator fun get(project: Project) =
            requireNotNull(project.getService(FuncInternalFactory::class.java))
    }
}