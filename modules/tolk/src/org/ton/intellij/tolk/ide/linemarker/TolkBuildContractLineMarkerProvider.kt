package org.ton.intellij.tolk.ide.linemarker

import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.ton.intellij.acton.ActonIcons
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandConfigurationType
import org.ton.intellij.tolk.psi.TolkContractDefinition
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFile
import javax.swing.Icon

class TolkBuildContractLineMarkerProvider : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != TolkElementTypes.IDENTIFIER) return null

        val contract = element.parent as? TolkContractDefinition ?: return null
        if (contract.nameIdentifier != element) return null

        val file = contract.containingFile.originalFile as? TolkFile ?: return null
        if (file.isTestFile() || file.isActonFile() || file.isInScriptsFolder()) return null

        val virtualFile = file.virtualFile ?: return null
        val actonToml = ActonToml.find(contract.project) ?: return null
        val contractId = actonToml.findContractIdBySource(virtualFile) ?: return null

        val definitionsInFile = PsiTreeUtil.getChildrenOfTypeAsList(file, TolkContractDefinition::class.java)
        if (definitionsInFile.size > 1 && contract.name != contractId) return null

        return Info(
            AllIcons.Actions.Rebuild,
            arrayOf(
                BuildActonContractAction(contractId),
                PlaceholderContractAction("Disassemble $contractId", ActonIcons.DISASSEMBLE),
                PlaceholderContractAction("Generate TypeScript Wrapper", ActonIcons.TS_WRAPPER),
            ),
        ) { "Build $contractId" }
    }

    private class BuildActonContractAction(private val contractId: String) :
        AnAction("Build $contractId", null, AllIcons.Actions.Compile) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val actonToml = ActonToml.find(project) ?: return

            val runManager = RunManager.getInstance(project)
            val settings = runManager.createConfiguration(
                "Build $contractId",
                ActonCommandConfigurationType.getInstance().factory,
            )
            val configuration = settings.configuration as ActonCommandConfiguration

            configuration.command = "build"
            configuration.workingDirectory = actonToml.workingDir
            configuration.buildContractId = contractId
            configuration.parameters = "--info"

            runManager.addConfiguration(settings)
            runManager.selectedConfiguration = settings

            ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
        }
    }

    private class PlaceholderContractAction(text: String, icon: Icon) : AnAction(text, null, icon) {
        override fun actionPerformed(e: AnActionEvent) = Unit
    }
}
