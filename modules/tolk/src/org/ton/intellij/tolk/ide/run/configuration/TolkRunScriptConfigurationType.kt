package org.ton.intellij.tolk.ide.run.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.TolkIcons

class TolkRunScriptConfigurationType : ConfigurationTypeBase(
    ID,
    TolkBundle.message("tolk.script.configuration.name"),
    TolkBundle.message("tolk.script.configuration.description"),
    TolkIcons.FILE
) {
    val factory: ConfigurationFactory = TolkRunScriptConfigurationFactory(this)

    init {
        addFactory(factory)
    }

    companion object {
        const val ID = "TolkRunScriptConfigurationType"

        fun getInstance(): TolkRunScriptConfigurationType {
            return ConfigurationTypeUtil.findConfigurationType(ID) as TolkRunScriptConfigurationType
        }
    }
}
