package org.ton.intellij.acton.ide.newProject

import com.intellij.execution.configuration.EnvironmentVariablesData

class ActonProjectSettings {
    companion object {
        const val DEFAULT_TEMPLATE: String = "counter"
        const val DEFAULT_DESCRIPTION: String = "A TON blockchain project"
        const val DEFAULT_LICENSE: String = "MIT"
    }

    var template: String = DEFAULT_TEMPLATE
    var addTypeScriptApp: Boolean = false
    var templateSupportsTypeScriptApp: Boolean = false
    var starterFilePath: String? = null
    var description: String = DEFAULT_DESCRIPTION
    var license: String = DEFAULT_LICENSE
    var includeGitHooks: Boolean = false
    var includeAgentsMd: Boolean = false
    var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
}
