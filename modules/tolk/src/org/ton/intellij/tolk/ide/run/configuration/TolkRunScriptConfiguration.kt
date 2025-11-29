package org.ton.intellij.tolk.ide.run.configuration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element

class TolkRunScriptConfiguration(
    project: Project,
    factory: ConfigurationFactory,
) : LocatableConfigurationBase<TolkRunScriptConfigurationRunState>(project, factory, "Tolk Script") {

    var filename: String = ""
    var additionalParameters: String = ""

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return TolkRunScriptConfigurationEditor()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return TolkRunScriptConfigurationRunState(environment, this)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.setAttribute("filename", filename)
        element.setAttribute("additionalParameters", additionalParameters)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        filename = element.getAttributeValue("filename") ?: ""
        additionalParameters = element.getAttributeValue("additionalParameters") ?: ""
    }
}
