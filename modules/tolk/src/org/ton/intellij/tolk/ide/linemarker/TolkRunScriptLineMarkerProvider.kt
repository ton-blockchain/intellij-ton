package org.ton.intellij.tolk.ide.linemarker

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.tolk.ide.run.configuration.ActonRunScriptBroadcastConfigurationProducer
import org.ton.intellij.tolk.ide.run.configuration.ActonRunScriptConfigurationProducer
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFunction

class TolkRunScriptLineMarkerProvider : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != TolkElementTypes.IDENTIFIER) return null

        val parent = element.parent
        if (parent is TolkFunction && parent.name == "main") {
            val actions = listOf(
                RunActonScriptAction(false),
                RunActonScriptAction(true)
            )
            return Info(AllIcons.RunConfigurations.TestState.Run, (actions + ExecutorAction.getActions(0)).toTypedArray()) { "Run Script" }
        }

        return null
    }

    private class RunActonScriptAction(private val broadcast: Boolean) : AnAction(
        if (broadcast) "Broadcast" else "Emulate",
        null,
        if (broadcast) AllIcons.General.Export else AllIcons.Ide.LocalScope
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            val context = ConfigurationContext.getFromContext(e.dataContext)
            val producer = if (broadcast) ActonRunScriptBroadcastConfigurationProducer() else ActonRunScriptConfigurationProducer()
            val configurationFromContext = producer.findOrCreateConfigurationFromContext(context) ?: return
            ExecutionUtil.runConfiguration(configurationFromContext.configurationSettings, DefaultRunExecutor.getRunExecutorInstance())
        }
    }
}
