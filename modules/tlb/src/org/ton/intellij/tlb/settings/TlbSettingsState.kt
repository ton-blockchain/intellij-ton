package org.ton.intellij.tlb.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "TlbSettings",
    storages = [Storage("tlb.xml")]
)
@Service(Service.Level.PROJECT)
class TlbSettingsState : PersistentStateComponent<TlbSettingsState> {
    var globalBlockTlbPath: String = ""

    override fun getState(): TlbSettingsState = this

    override fun loadState(state: TlbSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): TlbSettingsState = project.service<TlbSettingsState>()
    }
}
