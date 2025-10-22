package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.TolkIcons

class TolkTestConfigurationType : ConfigurationTypeBase(
    ID,
    TolkBundle.message("tolk.test.configuration.name"),
    TolkBundle.message("tolk.test.configuration.description"),
    TolkIcons.FILE
) {
    val factory: ConfigurationFactory = TolkTestConfigurationFactory(this)

    init {
        addFactory(factory)
    }

    companion object {
        const val ID = "TolkTestConfigurationType"

        fun getInstance(): TolkTestConfigurationType {
            return ConfigurationTypeUtil.findConfigurationType(ID) as TolkTestConfigurationType
        }
    }
}
