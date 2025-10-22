package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class TolkTestConfigurationFactory(type: TolkTestConfigurationType) : ConfigurationFactory(type) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return TolkTestConfiguration(project, this)
    }

    override fun getId(): String = "TolkTest"
}
