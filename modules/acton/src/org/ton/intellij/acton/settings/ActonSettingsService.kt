package org.ton.intellij.acton.settings

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

val Project.actonSettings: ActonSettingsService get() = service()

@State(
    name = "ActonSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
@Service(Service.Level.PROJECT)
class ActonSettingsService : SimplePersistentStateComponent<ActonSettingsService.ActonSettingsState>(ActonSettingsState()) {
    
    var actonPath: String?
        get() = state.actonPath
        set(value) {
            state.actonPath = value
        }

    var actonVersion: String?
        get() = state.actonVersion
        set(value) {
            state.actonVersion = value
        }

    var env: EnvironmentVariablesData
        get() = EnvironmentVariablesData.create(state.envs, state.isPassParentEnvs)
        set(value) {
            state.envs = value.envs.toMutableMap()
            state.isPassParentEnvs = value.isPassParentEnvs
        }

    class ActonSettingsState : BaseState() {
        var actonPath by string()
        var actonVersion by string()
        var envs by map<String, String>()
        var isPassParentEnvs by property(true)
    }
}
