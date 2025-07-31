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
