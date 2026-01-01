package org.ton.intellij.tolk.ide.run.configuration

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandConfigurationType
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.util.parentOfType

class ActonRunScriptBroadcastConfigurationProducer : LazyRunConfigurationProducer<ActonCommandConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        ActonCommandConfigurationType.getInstance().factory

    override fun isConfigurationFromContext(
        configuration: ActonCommandConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val element = context.location?.psiElement ?: return false
        val containingFile = element.containingFile as? TolkFile ?: return false
        val function = element.parentOfType<TolkFunction>() ?: return false
        
        if (function.name == "main") {
            val actonToml = ActonToml.find(configuration.project) ?: return false
            return configuration.command == "script" && 
                   configuration.scriptPath == containingFile.virtualFile.path &&
                   configuration.workingDirectory == actonToml.workingDir &&
                   configuration.scriptBroadcast
        }
        return false
    }

    override fun setupConfigurationFromContext(
        configuration: ActonCommandConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val element = sourceElement.get() ?: return false
        val containingFile = element.containingFile as? TolkFile ?: return false
        val function = element.parentOfType<TolkFunction>() ?: return false

        if (function.name == "main") {
            val actonToml = ActonToml.find(configuration.project) ?: return false
            configuration.name = "Broadcast ${containingFile.name}"
            configuration.command = "script"
            configuration.scriptPath = containingFile.virtualFile.path
            configuration.workingDirectory = actonToml.workingDir
            configuration.scriptBroadcast = true
            return true
        }
        return false
    }
}
