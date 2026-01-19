package org.ton.intellij.acton.settings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.ton.intellij.acton.settings.ActonExternalLinterSettingsService.ActonExternalLinterSettings

val Project.externalLinterSettings: ActonExternalLinterSettingsService
    get() = service<ActonExternalLinterSettingsService>()

private const val SERVICE_NAME: String = "ActonExternalLinterSettings"

@Service(Service.Level.PROJECT)
@State(name = SERVICE_NAME, storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class ActonExternalLinterSettingsService(
    project: Project
) : ActonProjectSettingsServiceBase<ActonExternalLinterSettings>(project, ActonExternalLinterSettings()) {

    val enabled: Boolean get() = state.enabled
    val additionalArguments: String get() = state.additionalArguments
    val envs: Map<String, String> get() = state.envs
    val isPassParentEnvs: Boolean get() = state.isPassParentEnvs

    class ActonExternalLinterSettings : ActonProjectSettingsBase<ActonExternalLinterSettings>() {
        @AffectsHighlighting
        var enabled by property(true)

        @AffectsHighlighting
        var additionalArguments by property("") { it.isEmpty() }

        @AffectsHighlighting
        var envs by map<String, String>()

        @AffectsHighlighting
        var isPassParentEnvs by property(true)

        override fun copy(): ActonExternalLinterSettings {
            val state = ActonExternalLinterSettings()
            state.copyFrom(this)
            return state
        }
    }

    override fun createSettingsChangedEvent(
        oldEvent: ActonExternalLinterSettings,
        newEvent: ActonExternalLinterSettings
    ): SettingsChangedEvent = SettingsChangedEvent(oldEvent, newEvent)

    class SettingsChangedEvent(
        oldState: ActonExternalLinterSettings,
        newState: ActonExternalLinterSettings
    ) : SettingsChangedEventBase<ActonExternalLinterSettings>(oldState, newState)
}