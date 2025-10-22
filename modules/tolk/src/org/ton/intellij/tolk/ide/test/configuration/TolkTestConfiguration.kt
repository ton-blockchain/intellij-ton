package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element

class TolkTestConfiguration(
    project: Project,
    factory: ConfigurationFactory
) : LocatableConfigurationBase<TolkTestConfigurationRunState>(project, factory, "Tolk Test") {

    var scope: TolkTestScope = TolkTestScope.File
    var directory: String = ""
    var filename: String = ""
    var pattern: String = ""
    var additionalParameters: String = ""

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return TolkTestConfigurationEditor()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return TolkTestConfigurationRunState(environment, this)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.setAttribute("scope", scope.name)
        element.setAttribute("directory", directory)
        element.setAttribute("filename", filename)
        element.setAttribute("pattern", pattern)
        element.setAttribute("additionalParameters", additionalParameters)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        scope = TolkTestScope.from(element.getAttributeValue("scope") ?: "")
        directory = element.getAttributeValue("directory") ?: ""
        filename = element.getAttributeValue("filename") ?: ""
        pattern = element.getAttributeValue("pattern") ?: ""
        additionalParameters = element.getAttributeValue("additionalParameters") ?: ""
    }
}
