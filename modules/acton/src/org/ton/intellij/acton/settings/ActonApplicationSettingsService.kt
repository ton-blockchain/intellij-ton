package org.ton.intellij.acton.settings

import com.intellij.openapi.components.*

val actonApplicationSettings: ActonApplicationSettingsService get() = service()

@State(
    name = "ActonApplicationSettings",
    storages = [Storage("acton.xml")]
)
@Service(Service.Level.APP)
class ActonApplicationSettingsService : SimplePersistentStateComponent<ActonApplicationSettingsService.ActonApplicationSettingsState>(ActonApplicationSettingsState()) {

    var disableUpdateChecks: Boolean
        get() = state.disableUpdateChecks
        set(value) {
            state.disableUpdateChecks = value
        }

    class ActonApplicationSettingsState : BaseState() {
        var disableUpdateChecks by property(false)
    }
}
