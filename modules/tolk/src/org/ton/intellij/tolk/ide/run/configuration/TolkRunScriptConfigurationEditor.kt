package org.ton.intellij.tolk.ide.run.configuration

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class TolkRunScriptConfigurationEditor : SettingsEditor<TolkRunScriptConfiguration>() {
    private val additionalParametersField = JBTextField()

    override fun resetEditorFrom(configuration: TolkRunScriptConfiguration) {
        additionalParametersField.text = configuration.additionalParameters
    }

    override fun applyEditorTo(configuration: TolkRunScriptConfiguration) {
        configuration.additionalParameters = additionalParametersField.text
    }

    override fun createEditor(): JComponent = panel {
        row("Additional parameters:") {
            cell(additionalParametersField)
                .align(AlignX.FILL)
        }
    }
}
