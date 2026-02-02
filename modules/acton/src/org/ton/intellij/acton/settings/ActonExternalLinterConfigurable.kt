package org.ton.intellij.acton.settings

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.ton.intellij.acton.ActonBundle
import javax.swing.JCheckBox

class ActonExternalLinterConfigurable(val project: Project) :
    BoundConfigurable(ActonBundle.message("settings.acton.external.linter.title.name")) {

    private val enabledCheckBox = JCheckBox(ActonBundle.message("settings.acton.external.linter.enabled.label"))
    private val additionalArgumentsField = ExpandableTextField()
    private val environmentVariables = EnvironmentVariablesComponent()

    override fun createPanel(): DialogPanel = panel {
        row {
            cell(enabledCheckBox)
                .comment(ActonBundle.message("settings.acton.external.linter.enabled.comment"))
        }

        row(ActonBundle.message("settings.acton.external.linter.additional.arguments.label")) {
            cell(additionalArgumentsField)
                .comment(ActonBundle.message("settings.acton.external.linter.additional.arguments.comment"))
                .align(Align.FILL)
        }

        row(environmentVariables.label) {
            cell(environmentVariables)
                .align(Align.FILL)
        }

        val settings = project.externalLinterSettings
        onApply {
            settings.modify {
                it.enabled = enabledCheckBox.isSelected
                it.additionalArguments = additionalArgumentsField.text
                it.envs = environmentVariables.envs
                it.isPassParentEnvs = environmentVariables.isPassParentEnvs
            }
        }
        onReset {
            enabledCheckBox.isSelected = settings.enabled
            additionalArgumentsField.text = settings.additionalArguments
            environmentVariables.envs = settings.envs
            environmentVariables.isPassParentEnvs = settings.isPassParentEnvs
        }
        onIsModified {
            enabledCheckBox.isSelected != settings.enabled ||
                    additionalArgumentsField.text != settings.additionalArguments ||
                    environmentVariables.envs != settings.envs ||
                    environmentVariables.isPassParentEnvs != settings.isPassParentEnvs
        }
    }
}
