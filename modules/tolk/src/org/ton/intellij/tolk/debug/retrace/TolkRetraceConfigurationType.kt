package org.ton.intellij.tolk.debug.retrace

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.ton.intellij.acton.ActonIcons
import org.ton.intellij.acton.cli.ActonToml

class TolkRetraceConfigurationType : ConfigurationTypeBase(
    "TolkRetraceRunConfiguration",
    "Tolk Retrace",
    "Debug Tolk retrace sessions via acton retrace",
    ActonIcons.ACTON
) {
    init {
        addFactory(TolkRetraceConfigurationFactory(this))
    }

    val factory: ConfigurationFactory get() = configurationFactories.single()

    companion object {
        fun getInstance(): TolkRetraceConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(TolkRetraceConfigurationType::class.java)
    }
}

class TolkRetraceConfigurationFactory(type: TolkRetraceConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "Tolk Retrace"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        val configuration = TolkRetraceConfiguration(project, this, "Tolk Retrace")
        configuration.workingDirectory = ActonToml.find(project)?.workingDir
        return configuration
    }
}
