package org.ton.intellij.tolk.ide.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project

class TolkRunScriptConfigurationFactory(type: TolkRunScriptConfigurationType) : ConfigurationFactory(type) {
    override fun createTemplateConfiguration(project: Project) = TolkRunScriptConfiguration(project, this)
    override fun getId(): String = "TolRunScript"
}
