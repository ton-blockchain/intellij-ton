package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class TolkTestConfigurationEditor : SettingsEditor<TolkTestConfiguration>() {

    private val additionalParametersField = JBTextField()

    override fun resetEditorFrom(configuration: TolkTestConfiguration) {
        additionalParametersField.text = configuration.additionalParameters
    }

    override fun applyEditorTo(configuration: TolkTestConfiguration) {
        configuration.additionalParameters = additionalParametersField.text
    }

    override fun createEditor(): JComponent = panel {
        row("Additional parameters:") {
            cell(additionalParametersField)
                .align(AlignX.FILL)
        }
    }
}
