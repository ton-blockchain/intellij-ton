package org.ton.intellij.acton.settings

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.Topic
import org.jetbrains.annotations.TestOnly
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

abstract class ActonProjectSettingsServiceBase<T : ActonProjectSettingsServiceBase.ActonProjectSettingsBase<T>>(
    val project: Project,
    state: T
) : SimplePersistentStateComponent<T>(state) {

    abstract class ActonProjectSettingsBase<T : ActonProjectSettingsBase<T>> : BaseState() {
        abstract fun copy(): T
    }

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.PROPERTY)
    protected annotation class AffectsHighlighting

    fun modify(action: (T) -> Unit) {
        val oldState = state.copy()
        val newState = state.also(action)
        val event = createSettingsChangedEvent(oldState, newState)
        notifySettingsChanged(event)
    }

    companion object {
        val ACTON_SETTINGS_TOPIC: Topic<ActonSettingsListener> = Topic.create(
            "acton settings changes",
            ActonSettingsListener::class.java,
            Topic.BroadcastDirection.TO_PARENT
        )
    }

    interface ActonSettingsListener {
        fun <T : ActonProjectSettingsBase<T>> settingsChanged(e: SettingsChangedEventBase<T>)
    }

    protected abstract fun createSettingsChangedEvent(oldEvent: T, newEvent: T): SettingsChangedEventBase<T>

    protected open fun notifySettingsChanged(event: SettingsChangedEventBase<T>) {
        project.messageBus.syncPublisher(ACTON_SETTINGS_TOPIC).settingsChanged(event)

        if (event.affectsHighlighting) {
            DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }

    abstract class SettingsChangedEventBase<T : ActonProjectSettingsBase<T>>(val oldState: T, val newState: T) {
        private val highlightingAffectingProps: List<KProperty1<T, *>> =
            oldState.javaClass.kotlin.memberProperties.filter { it.findAnnotation<AffectsHighlighting>() != null }

        val affectsHighlighting: Boolean
            get() = highlightingAffectingProps.any(::isChanged)

        /** Use it like `event.isChanged(State::foo)` to check whether `foo` property is changed or not */
        fun isChanged(prop: KProperty1<T, *>): Boolean = prop.get(oldState) != prop.get(newState)
    }
}