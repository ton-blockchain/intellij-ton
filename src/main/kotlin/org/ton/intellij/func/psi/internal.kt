package org.ton.intellij.func.psi

import com.intellij.openapi.project.Project

class FuncInternalFactory(project: Project) {
    val psiFactory = FuncPsiFactory(project)

    val stdLib by lazy {
        val text = FuncStdlib.text
        psiFactory.createFile(text, "stdlib", true).also {
            it.virtualFile.isWritable = false
        }
    }

    companion object {
        operator fun get(project: Project) =
            requireNotNull(project.getService(FuncInternalFactory::class.java))
    }
}
