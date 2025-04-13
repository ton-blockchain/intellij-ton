package org.ton.intellij.tolk.ide.configurable

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.ide.settings.tolkSettings
import org.ton.intellij.tolk.sdk.TolkSdkManager
import org.ton.intellij.tolk.sdk.TolkSdkRef

class TolkProjectConfigurable(
    val project: Project,
) : BoundConfigurable(TolkBundle.message("tolk.name")) {
    override fun createPanel(): DialogPanel = panel {
        val settings = project.tolkSettings
        val state = settings.state.copy()
        var sdkRef = TolkSdkManager[project].getSdkRef()

//        row(TolkBundle.message("tolk.language.level")) {
//            comboBox(EnumComboBoxModel(TolkLanguageLevel::class.java).apply {
//                this.setSelectedItem(state.languageLevel)
//            }, SimpleListCellRenderer.create("") { it.displayName })
//        }
        row("Tolk stdlib:") {
            textFieldWithBrowseButton(fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor())
                .bindText({
                    sdkRef.toString()
                }, {
                    sdkRef = TolkSdkRef(it)
                })
        }

        onApply {
            settings.state.also {
                it.languageLevel = state.languageLevel
                TolkSdkManager[project].setSdkRef(sdkRef)
            }
        }
    }
}
