package org.ton.intellij.acton.ide.newProject

import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.roots.ModifiableRootModel
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import java.nio.file.Paths

class ActonModuleBuilder : ModuleBuilder() {
    var configurationData: ActonProjectSettings? = null

    override fun getModuleType(): ModuleType<*> = ActonModuleType.INSTANCE

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val root = doAddContentEntry(modifiableRootModel)?.file ?: return
        val settings = configurationData ?: return

        val project = modifiableRootModel.project
        val projectName = root.name
        val command = ActonCommand.New(
            path = ".",
            projectName = projectName,
            description = "A TON blockchain project",
            template = settings.template,
            license = settings.license
        )
        val commandLine = ActonCommandLine(
            command = command.name,
            workingDirectory = Paths.get(root.path),
            additionalArguments = command.getArguments(),
            environmentVariables = settings.env
        ).toGeneralCommandLine(project)

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                val handler = CapturingProcessHandler(commandLine)
                ProcessTerminatedListener.attach(handler)
                handler.runProcess(30000)
            },
            "Generating Acton Project",
            true,
            project
        )
        
        root.refresh(false, true)

        val fileToOpen = when (settings.template) {
            "empty" -> "contracts/contract.tolk"
            "counter" -> "contracts/counter.tolk"
            "jetton" -> "contracts/jetton-minter-contract.tolk"
            else -> null
        }

        if (fileToOpen != null) {
            invokeLater {
                val file = root.findFileByRelativePath(fileToOpen)
                if (file != null) {
                    PsiNavigationSupport.getInstance().createNavigatable(project, file, -1).navigate(true)
                }
            }
        }
    }
}
