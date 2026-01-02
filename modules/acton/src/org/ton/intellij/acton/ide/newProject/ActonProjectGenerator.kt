package org.ton.intellij.acton.ide.newProject

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.ui.dsl.builder.*
import org.ton.intellij.acton.ActonIcons
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import java.nio.file.Paths
import javax.swing.Icon
import javax.swing.JComponent

class ActonProjectGenerator : DirectoryProjectGeneratorBase<ActonProjectSettings>() {
    override fun getName(): String = "Acton"

    override fun getDescription(): String = "Create a new Acton project"

    override fun getLogo(): Icon = ActonIcons.ACTON

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        settings: ActonProjectSettings,
        module: com.intellij.openapi.module.Module,
    ) {
        val projectName = baseDir.name
        val command = ActonCommand.New(
            path = ".",
            projectName = projectName,
            description = "A TON blockchain project",
            template = settings.template,
            license = settings.license
        )
        val commandLine = ActonCommandLine(
            command = command.name,
            workingDirectory = Paths.get(baseDir.path),
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

        baseDir.refresh(false, true)

        val fileToOpen = when (settings.template) {
            "empty" -> "contracts/contract.tolk"
            "counter" -> "contracts/counter.tolk"
            "jetton" -> "contracts/jetton-minter-contract.tolk"
            else -> null
        }

        if (fileToOpen != null) {
            invokeLater {
                val file = baseDir.findFileByRelativePath(fileToOpen)
                if (file != null) {
                    PsiNavigationSupport.getInstance().createNavigatable(project, file, -1).navigate(true)
                }
            }
        }

        createDefaultRunConfigurations(project, baseDir)
    }

    override fun createPeer(): ProjectGeneratorPeer<ActonProjectSettings> = ActonProjectGeneratorPeer()
}

class ActonProjectGeneratorPeer : GeneratorPeerImpl<ActonProjectSettings>() {
    private val propertyGraph = PropertyGraph()
    private val template = propertyGraph.property("counter")
    private val license = propertyGraph.property("MIT")
    private val environmentVariables = EnvironmentVariablesComponent()

    private var checkValid: Runnable? = null

    private val settings = ActonProjectSettings()

    init {
        template.afterChange { checkValid?.run() }
        license.afterChange { checkValid?.run() }
    }

    override fun getComponent(myLocationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        this.checkValid = checkValid
        return panel {
            row("Template:") {
                comboBox(listOf("empty", "counter", "jetton"))
                    .bindItem(template)
                    .align(AlignX.FILL)
            }.bottomGap(BottomGap.NONE)
            row("License:") {
                comboBox(listOf("MIT", "Apache-2.0", "GPL-3.0", "BSD-3-Clause", "ISC", "Unlicense"))
                    .bindItem(license)
                    .align(AlignX.FILL)
            }.bottomGap(BottomGap.NONE)
            row(environmentVariables.label) {
                cell(environmentVariables)
                    .align(AlignX.FILL)
            }
        }
    }

    override fun getSettings(): ActonProjectSettings = settings.apply {
        template = this@ActonProjectGeneratorPeer.template.get()
        license = this@ActonProjectGeneratorPeer.license.get()
        env = environmentVariables.envData
    }

    override fun validate(): com.intellij.openapi.ui.ValidationInfo? = null

    override fun buildUI(settingsStep: SettingsStep) {
        settingsStep.addSettingsComponent(component)
    }
}
