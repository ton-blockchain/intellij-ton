package org.ton.intellij.tolk.coverage

import com.intellij.coverage.CoverageRunner
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration

class TolkCoverageEnabledConfiguration(configuration: RunConfigurationBase<*>)
    : CoverageEnabledConfiguration(configuration, TolkCoverageRunner()) {

    init {
        coverageRunner = CoverageRunner.getInstance(TolkCoverageRunner::class.java)
    }
}
