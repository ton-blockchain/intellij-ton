package org.ton.intellij.func.ide.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import org.ton.intellij.func.FuncLanguageLevel

val Project.funcSettings: FuncProjectSettingsService
    get() = getService<FuncProjectSettingsService>(FuncProjectSettingsService::class.java)

private const val SERVICE_NAME: String = "FuncProjectSettings"

@Service(Service.Level.PROJECT)
@State(
    name = SERVICE_NAME, storages = [
        Storage(StoragePathMacros.WORKSPACE_FILE)
    ]
)
class FuncProjectSettingsService(
    val project: Project
) : SimplePersistentStateComponent<FuncProjectSettings>(FuncProjectSettings())

class FuncProjectSettings : BaseState() {
    var languageLevel by enum(FuncLanguageLevel.FUNC_0_4_0)

    fun copy() = FuncProjectSettings().also {
        it.languageLevel = languageLevel
    }
}
