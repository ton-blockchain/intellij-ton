package org.ton.intellij.func.psi

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.messages.Topic

/** Don't subscribe directly or via plugin.xml lazy listeners. Use [FuncPsiManager.subscribeFuncStructureChange] */
private val FUNC_STRUCTURE_CHANGE_TOPIC: Topic<FuncStructureChangeListener> = Topic.create(
    "FUNC_STRUCTURE_CHANGE_TOPIC",
    FuncStructureChangeListener::class.java,
    Topic.BroadcastDirection.TO_PARENT
)

interface FuncPsiManager {
    /**
     * A project-global modification tracker that increments on each PSI change that can affect
     * name resolution or type inference. It will be incremented with a change of most types of
     * PSI element excluding function bodies (expressions and statements)
     */
    val funcStructureModificationTracker: ModificationTracker

    /** This is an instance method because [FuncPsiManager] should be created prior to event subscription */
    fun subscribeFuncStructureChange(connection: MessageBusConnection, listener: FuncStructureChangeListener) {
        connection.subscribe(FUNC_STRUCTURE_CHANGE_TOPIC, listener)
    }
}

interface FuncStructureChangeListener {
    fun funcStructureChanged(file: PsiFile?, changedElement: PsiElement?)
}

//class FuncPsiManagerImpl(val project: Project) : FuncPsiManager, Disposable {
//    override val funcStructureModificationTracker = SimpleModificationTracker()
//
//    init {
//        PsiManager.getInstance(project).addPsiTreeChangeListener()
//    }
//
//    override fun dispose() {
//    }
//
//    private fun incFuncStructureModificationCount(file: PsiFile? = null, psiElement: PsiElement? = null) {
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
//                if (element is FuncFile) {
//                    val funcFile = element as? FuncFile
//                    incFuncStructureModificationCount(funcFile, funcFile)
//                }
//            } else {
//                if (file.fileType != FuncFileType) return
//                val isWhitespaceOrComment = element is PsiComment || element is PsiWhiteSpace
//                if (isWhitespaceOrComment) return
//
//                val owner = if (DumbService.isDumb(project)) null else element.findModificationTrackerOwner(!isChildrenChange)
//
//            }
//        }
//    }
//}
