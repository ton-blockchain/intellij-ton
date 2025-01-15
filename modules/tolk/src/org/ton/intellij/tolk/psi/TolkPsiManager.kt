package org.ton.intellij.tolk.psi

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.messages.Topic

/** Don't subscribe directly or via plugin.xml lazy listeners. Use [TolkPsiManager.subscribeTolkStructureChange] */
private val FUNC_STRUCTURE_CHANGE_TOPIC: Topic<TolkStructureChangeListener> = Topic.create(
    "FUNC_STRUCTURE_CHANGE_TOPIC",
    TolkStructureChangeListener::class.java,
    Topic.BroadcastDirection.TO_PARENT
)

interface TolkPsiManager {
    /**
     * A project-global modification tracker that increments on each PSI change that can affect
     * name resolution or type inference. It will be incremented with a change of most types of
     * PSI element excluding function bodies (expressions and statements)
     */
    val funcStructureModificationTracker: ModificationTracker

    /** This is an instance method because [TolkPsiManager] should be created prior to event subscription */
    fun subscribeTolkStructureChange(connection: MessageBusConnection, listener: TolkStructureChangeListener) {
        connection.subscribe(FUNC_STRUCTURE_CHANGE_TOPIC, listener)
    }
}

interface TolkStructureChangeListener {
    fun funcStructureChanged(file: PsiFile?, changedElement: PsiElement?)
}

//class TolkPsiManagerImpl(val project: Project) : TolkPsiManager, Disposable {
//    override val funcStructureModificationTracker = SimpleModificationTracker()
//
//    init {
//        PsiManager.getInstance(project).addPsiTreeChangeListener()
//    }
//
//    override fun dispose() {
//    }
//
//    private fun incTolkStructureModificationCount(file: PsiFile? = null, psiElement: PsiElement? = null) {
//        funcStructureModificationTracker.incModificationCount()
//        project.messageBus.syncPublisher(FUNC_STRUCTURE_CHANGE_TOPIC).funcStructureChanged(file, psiElement)
//    }
//
//    inner class CacheInvalidator : PsiTreeChangeListener {
//        override fun beforeChildAddition(event: PsiTreeChangeEvent) {
//
//        }
//
//        override fun beforeChildRemoval(event: PsiTreeChangeEvent) {
//        }
//
//        override fun beforeChildReplacement(event: PsiTreeChangeEvent) {
//        }
//
//        override fun beforeChildMovement(event: PsiTreeChangeEvent) {
//        }
//
//        override fun beforeChildrenChange(event: PsiTreeChangeEvent) {
//        }
//
//        override fun beforePropertyChange(event: PsiTreeChangeEvent) {
//        }
//
//        override fun childAdded(event: PsiTreeChangeEvent) {
//        }
//
//        override fun childRemoved(event: PsiTreeChangeEvent) {
//        }
//
//        override fun childReplaced(event: PsiTreeChangeEvent) {
//        }
//
//        override fun childrenChanged(event: PsiTreeChangeEvent) {
//        }
//
//        override fun childMoved(event: PsiTreeChangeEvent) {
//        }
//
//        override fun propertyChanged(event: PsiTreeChangeEvent) {
//        }
//
//        fun filterEvent(file: PsiFile?, element: PsiElement, isChildrenChange: Boolean) {
//            // if file is null, this is an event about VFS changes
//            if (file == null) {
//                if (element is TolkFile) {
//                    val funcFile = element as? TolkFile
//                    incTolkStructureModificationCount(funcFile, funcFile)
//                }
//            } else {
//                if (file.fileType != TolkFileType) return
//                val isWhitespaceOrComment = element is PsiComment || element is PsiWhiteSpace
//                if (isWhitespaceOrComment) return
//
//                val owner = if (DumbService.isDumb(project)) null else element.findModificationTrackerOwner(!isChildrenChange)
//
//            }
//        }
//    }
//}
