package org.ton.intellij.tolk.ide.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import org.ton.intellij.tolk.TolkLanguageLevel

val Project.funcSettings: TolkProjectSettingsService
    get() = service<TolkProjectSettingsService>()

private const val SERVICE_NAME: String = "TolkProjectSettings"

@Service(Service.Level.PROJECT)
@State(
    name = SERVICE_NAME, storages = [
        Storage(StoragePathMacros.WORKSPACE_FILE)
    ]
)
class TolkProjectSettingsService(
    val project: Project
) : SimplePersistentStateComponent<TolkProjectSettings>(TolkProjectSettings()) {

}

class TolkProjectSettings : BaseState() {
    var languageLevel by enum(TolkLanguageLevel.TOLK_0_1)

    fun copy() = TolkProjectSettings().also {
        it.languageLevel = languageLevel
    }
}
