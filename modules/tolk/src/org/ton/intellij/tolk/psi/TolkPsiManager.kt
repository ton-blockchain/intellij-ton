package org.ton.intellij.tolk.psi

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.*
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.messages.Topic
import org.ton.intellij.tolk.TolkFileType

private val TOLK_SIGNATURE_CHANGE_TOPIC = Topic.create(
    "TOLK_SIGNATURE_CHANGE_TOPIC",
    TolkSignatureChangeListener::class.java,
    Topic.BroadcastDirection.TO_PARENT
)

private val TOLK_PSI_CHANGE_TOPIC = Topic.create(
    "TOLK_PSI_CHANGE_TOPIC",
    TolkPsiChangeListener::class.java,
    Topic.BroadcastDirection.TO_PARENT
)

fun interface TolkSignatureChangeListener {
    fun tolkSignatureChanged(file: PsiFile?, changedElement: PsiElement?)
}

fun interface TolkPsiChangeListener {
    fun tolkPsiChanged(file: PsiFile, element: PsiElement, isStructureModification: Boolean)
}

@Service(Service.Level.PROJECT)
class TolkPsiManager(
    val project: Project
) : Disposable {
    val tolkStructureModificationCount = SimpleModificationTracker()
    private val tolkCacheInvalidator = TolkCacheInvalidator()
    private val tolkModuleRootListener = TolkModuleRootListener()

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(tolkCacheInvalidator, this)
        project.messageBus.connect().subscribe(ModuleRootListener.TOPIC, tolkModuleRootListener)
    }

    override fun dispose() {}

    fun subscribeTolkSignatureChange(
        connectionManager: MessageBusConnection,
        listener: TolkSignatureChangeListener,
    ) {
        connectionManager.subscribe(TOLK_SIGNATURE_CHANGE_TOPIC, listener)
    }

    fun subscribeTolkPsiChange(
        connectionManager: MessageBusConnection,
        listener: TolkPsiChangeListener,
    ) {
        connectionManager.subscribe(TOLK_PSI_CHANGE_TOPIC, listener)
    }

    inner class TolkModuleRootListener : ModuleRootListener {
        override fun rootsChanged(event: ModuleRootEvent) {
            incTolkSignatureModificationCount()
        }
    }

    inner class TolkCacheInvalidator : PsiTreeChangeListener {
        override fun beforeChildAddition(event: PsiTreeChangeEvent) = Unit

        override fun beforeChildRemoval(event: PsiTreeChangeEvent) = handle(event.file, event.child)

        override fun beforeChildReplacement(event: PsiTreeChangeEvent) = handle(event.file, event.oldChild)

        override fun beforeChildMovement(event: PsiTreeChangeEvent) = Unit

        override fun beforeChildrenChange(event: PsiTreeChangeEvent) = Unit

        override fun beforePropertyChange(event: PsiTreeChangeEvent) = Unit

        override fun childAdded(event: PsiTreeChangeEvent) = handle(event.file, event.child)

        override fun childRemoved(event: PsiTreeChangeEvent) = handle(event.file, event.parent, isChildrenChange = true)

        override fun childReplaced(event: PsiTreeChangeEvent) = handle(event.file, event.newChild)

        override fun childrenChanged(event: PsiTreeChangeEvent) {
            val element = if ((event as? PsiTreeChangeEventImpl)?.isGenericChange != true) event.parent else return
            handle(event.file, element, isChildrenChange = true)
        }

        override fun childMoved(event: PsiTreeChangeEvent) = handle(event.file, event.child)

        override fun propertyChanged(event: PsiTreeChangeEvent) = Unit

        fun handle(file: PsiFile?, element: PsiElement, isChildrenChange: Boolean = false) {
            // if file is null, this is an event about VFS changes
            if (file == null) {
                val isSignatureModification = element is TolkFile || element is PsiDirectory
                if (isSignatureModification) {
                    incTolkSignatureModificationCount(element as? TolkFile, element as? TolkFile)
                }
            } else {
                if (file.fileType != TolkFileType) return
                val isWhitespaceOrComment = element is PsiWhiteSpace || element is PsiComment
                if (isWhitespaceOrComment) {
                    // Ignore whitespace and comments changes
                    return
                }
                updateModificationCount(file, element, isChildrenChange)
            }
        }
    }

    fun incTolkSignatureModificationCount(file: PsiFile? = null, psi: PsiElement? = null) {
        tolkStructureModificationCount.incModificationCount()
        project.messageBus.syncPublisher(TOLK_SIGNATURE_CHANGE_TOPIC).tolkSignatureChanged(file, psi)
    }

    private fun updateModificationCount(
        file: PsiFile,
        psi: PsiElement,
        isChildrenChange: Boolean,
    ) {
        val owner = if (DumbService.isDumb(project)) null else psi.findTolkModificationTrackerOwner(!isChildrenChange)
        val isStructureModification = owner == null || !owner.incModificationCount(psi)
        if (isStructureModification) {
            incTolkSignatureModificationCount(file, psi)
        }
        project.messageBus.syncPublisher(TOLK_PSI_CHANGE_TOPIC).tolkPsiChanged(file, psi, isStructureModification)
    }

    companion object {
        private val LOG = logger<TolkPsiManager>()
    }
}

val Project.tolkPsiManager: TolkPsiManager get() = service()
