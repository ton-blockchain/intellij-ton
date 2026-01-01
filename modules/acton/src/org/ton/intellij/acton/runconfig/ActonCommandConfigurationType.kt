package org.ton.intellij.acton.runconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader

class ActonCommandConfigurationType : ConfigurationTypeBase(
    "ActonCommandRunConfiguration",
    "Acton",
    "Acton command run configuration",
    IconLoader.getIcon("/icons/acton.svg", ActonCommandConfigurationType::class.java)
) {
    init {
        addFactory(ActonConfigurationFactory(this))
    }

    val factory: ConfigurationFactory get() = configurationFactories.single()

    companion object {
        fun getInstance(): ActonCommandConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(ActonCommandConfigurationType::class.java)
    }
}

class ActonConfigurationFactory(type: ActonCommandConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "Acton Command"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return ActonCommandConfiguration(project, this, "Acton")
    }
}
