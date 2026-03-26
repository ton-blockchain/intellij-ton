package org.ton.intellij.acton.ide.newProject

import com.intellij.execution.configuration.EnvironmentVariablesData

class ActonProjectSettings {
    var template: String = "counter"
    var addTypeScriptApp: Boolean = false
    var license: String = "MIT"
    var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
}
