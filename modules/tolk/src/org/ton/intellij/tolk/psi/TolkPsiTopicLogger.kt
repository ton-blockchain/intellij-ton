package org.ton.intellij.tolk.psi

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class TolkPsiTopicLogger(
    val project: Project
) : Disposable, TolkPsiChangeListener, TolkStructureChangeListener {
    init {
        val connection = project.messageBus.connect(this)
        val tolkPsiManager = project.tolkPsiManager
        tolkPsiManager.subscribeTolkPsiChange(connection, this)
        tolkPsiManager.subscribeTolkStructureChange(connection, this)
    }

    override fun dispose() {
    }

    override fun tolkPsiChanged(
        file: PsiFile,
        element: PsiElement,
        isStructureModification: Boolean
    ) {
        LOG.warn("psi changed ${file.name} $element isStructureModification=$isStructureModification")
    }

    override fun tolkStructureChanged(file: PsiFile?, changedElement: PsiElement?) {
        LOG.warn("structure changed ${file?.name} $changedElement")
    }

    companion object {
        private val LOG = logger<TolkPsiTopicLogger>()
    }
}
