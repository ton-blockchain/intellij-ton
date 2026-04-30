package org.ton.intellij.tolk.ide.linemarker

import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandConfigurationType
import org.ton.intellij.asm.AsmFileType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.ide.assembly.TolkAssemblyPreviewManager
import org.ton.intellij.tolk.psi.TolkContractDefinition
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFile

class TolkBuildContractLineMarkerProvider : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != TolkElementTypes.IDENTIFIER) return null

        val contract = element.parent as? TolkContractDefinition ?: return null
        if (contract.nameIdentifier != element) return null

        val file = contract.containingFile.originalFile as? TolkFile ?: return null
        if (file.isTestFile() || file.isActonFile() || file.isInScriptsFolder()) return null

        val virtualFile = file.virtualFile ?: return null
        val actonToml = ActonToml.find(contract.project, virtualFile) ?: return null
        val contractId = actonToml.findContractIdBySource(virtualFile) ?: return null

        val definitionsInFile = PsiTreeUtil.getChildrenOfTypeAsList(file, TolkContractDefinition::class.java)
        if (definitionsInFile.size > 1 && contract.name != contractId) return null

        return Info(
            AllIcons.Actions.Rebuild,
            arrayOf(
                BuildActonContractAction(contractId, virtualFile),
                DisassembleActonContractAction(virtualFile),
                GenerateTolkWrapperAction(contractId, virtualFile),
                GenerateTypeScriptWrapperAction(contractId, virtualFile),
            ),
        ) { "Build, disassemble, or generate wrappers" }
    }

    private class BuildActonContractAction(private val contractId: String, private val sourceFile: VirtualFile) :
        AnAction("Build", null, AllIcons.Actions.Compile) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val actonToml = ActonToml.find(project, sourceFile) ?: return

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

            ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
        }
    }

    private class DisassembleActonContractAction(private val sourceFile: VirtualFile) :
        AnAction("Disassemble", null, AsmFileType.icon) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            TolkAssemblyPreviewManager.open(project, sourceFile)
        }
    }

    private class GenerateTolkWrapperAction(private val contractId: String, private val sourceFile: VirtualFile) :
        AnAction("Generate Tolk Wrapper", null, TolkIcons.FILE) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val actonToml = ActonToml.find(project, sourceFile) ?: return

            val runManager = RunManager.getInstance(project)
            val settings = runManager.createConfiguration(
                "Generate Tolk wrapper for $contractId",
                ActonCommandConfigurationType.getInstance().factory,
            )
            val configuration = settings.configuration as ActonCommandConfiguration

            configuration.command = "wrapper"
            configuration.workingDirectory = actonToml.workingDir
            configuration.parameters = contractId

            ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
        }
    }

    private class GenerateTypeScriptWrapperAction(private val contractId: String, private val sourceFile: VirtualFile) :
        AnAction("Generate TypeScript Wrapper", null, TolkIcons.TYPESCRIPT) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val actonToml = ActonToml.find(project, sourceFile) ?: return

            val runManager = RunManager.getInstance(project)
            val settings = runManager.createConfiguration(
                "Generate TypeScript wrapper for $contractId",
                ActonCommandConfigurationType.getInstance().factory,
            )
            val configuration = settings.configuration as ActonCommandConfiguration

            configuration.command = "wrapper"
            configuration.workingDirectory = actonToml.workingDir
            configuration.parameters = "--ts $contractId"

            ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
        }
    }
}
