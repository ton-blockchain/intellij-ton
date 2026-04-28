@file:Suppress("DEPRECATION")

package org.ton.intellij.acton.ide.newProject

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import org.ton.intellij.acton.ActonIcons
import org.ton.intellij.acton.cli.ActonCommandLine
import java.awt.BorderLayout
import java.awt.Cursor
import java.nio.file.Paths
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class ActonProjectGenerator : DirectoryProjectGeneratorBase<ActonProjectSettings>() {
    override fun getName(): String = "TON"

    override fun getDescription(): String = "Create a new TON project with Acton"

    override fun getLogo(): Icon = ActonIcons.TON

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        settings: ActonProjectSettings,
        module: com.intellij.openapi.module.Module,
    ) {
        val projectName = baseDir.name
        val command = createActonNewCommand(projectName, settings)
        val commandLine = ActonCommandLine(
            command = command.name,
            workingDirectory = Paths.get(baseDir.path),
            additionalArguments = command.getArguments(),
            environmentVariables = settings.env,
        ).toGeneralCommandLine(project) ?: return

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                val handler = CapturingProcessHandler(commandLine)
                ProcessTerminatedListener.attach(handler)
                handler.runProcess(30000)
            },
            "Generating Acton Project",
            true,
            project,
        )

        baseDir.refresh(false, true)
        VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)

        val fileToOpen = starterFilePathForTemplate(
            template = settings.template,
            includeTypeScriptApp = settings.addTypeScriptApp,
        )

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

internal class ActonProjectGeneratorPeer(
    private val templateCatalog: ActonTemplateCatalog = ActonTemplateCatalogProvider.getTemplateCatalog(),
) : GeneratorPeerImpl<ActonProjectSettings>() {
    private val templateOptions = templateCatalog.templateIds()
    private val propertyGraph = PropertyGraph()
    private val template = propertyGraph.property(templateCatalog.normalizedDefaultTemplate())
    private val addTypeScriptApp = propertyGraph.property(false)
    private val showAdvancedOptions = propertyGraph.property(false)
    private val description = propertyGraph.property(ActonProjectSettings.DEFAULT_DESCRIPTION)
    private val license = propertyGraph.property(ActonProjectSettings.DEFAULT_LICENSE)
    private val includeGitHooks = propertyGraph.property(false)
    private val includeAgentsMd = propertyGraph.property(false)
    private val gitAvailable = PathEnvironmentVariableUtil.findInPath("git") != null
    private val environmentVariables = EnvironmentVariablesComponent()
    private var addTypeScriptAppRow: Row? = null

    private var checkValid: Runnable? = null

    private val settings = ActonProjectSettings()

    init {
        template.afterChange {
            updateAddTypeScriptAppVisibility()
            checkValid?.run()
        }
        license.afterChange { checkValid?.run() }
    }

    override fun getComponent(myLocationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        this.checkValid = checkValid
        return panel {
            row("Template:") {
                comboBox(templateOptions)
                    .bindItem(template)
                    .align(AlignX.FILL)
            }.bottomGap(BottomGap.NONE)
            addTypeScriptAppRow = row {
                checkBox("Add TypeScript app")
                    .bindSelected(addTypeScriptApp)
                    .comment("Include the template's TypeScript app scaffold")
            }
            row(environmentVariables.label) {
                cell(environmentVariables)
                    .align(AlignX.FILL)
            }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)
            row {
                cell(createAdvancedOptionsPanel())
                    .align(AlignX.FILL)
            }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)
        }.also {
            updateAddTypeScriptAppVisibility()
        }
    }

    override fun getSettings(): ActonProjectSettings = settings.apply {
        template = this@ActonProjectGeneratorPeer.template.get()
        addTypeScriptApp = templateCatalog.supportsTypeScriptApp(template) &&
            this@ActonProjectGeneratorPeer.addTypeScriptApp.get()
        description = this@ActonProjectGeneratorPeer.description.get()
        license = this@ActonProjectGeneratorPeer.license.get()
        includeGitHooks = gitAvailable && this@ActonProjectGeneratorPeer.includeGitHooks.get()
        includeAgentsMd = this@ActonProjectGeneratorPeer.includeAgentsMd.get()
        env = environmentVariables.envData
    }

    private fun updateAddTypeScriptAppVisibility() {
        addTypeScriptAppRow?.visible(templateCatalog.supportsTypeScriptApp(template.get()))
    }

    private fun createAdvancedOptionsPanel(): JComponent {
        val content = panel {
            row("Description:") {
                textField()
                    .bindText(description)
                    .align(AlignX.FILL)
            }
            row("License:") {
                comboBox(LICENSE_OPTIONS)
                    .bindItem(license)
                    .align(AlignX.FILL)
            }
            row {
                val gitHooksCell = checkBox("Install Git hooks")
                    .bindSelected(includeGitHooks)
                if (gitAvailable) {
                    gitHooksCell.comment("Create and install the default project-local hooks")
                } else {
                    gitHooksCell.enabled(false)
                        .comment("Requires `git` in PATH")
                }
            }
            row {
                checkBox("Include AGENTS.md")
                    .bindSelected(includeAgentsMd)
                    .comment("Add coding-agent guidance to the scaffold")
            }
        }

        val contentWrapper = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyLeft(20)
            add(content, BorderLayout.CENTER)
        }
        val disclosureButton = JButton("Advanced options").apply {
            isBorderPainted = false
            isContentAreaFilled = false
            isFocusPainted = false
            horizontalAlignment = SwingConstants.LEFT
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            margin = JBUI.emptyInsets()
        }

        fun updateDisclosureState() {
            val expanded = showAdvancedOptions.get()
            disclosureButton.icon = if (expanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
            contentWrapper.isVisible = expanded
            contentWrapper.revalidate()
            contentWrapper.repaint()
        }

        disclosureButton.addActionListener {
            showAdvancedOptions.set(!showAdvancedOptions.get())
            updateDisclosureState()
            checkValid?.run()
        }

        return JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            isOpaque = false
            add(disclosureButton, BorderLayout.NORTH)
            add(contentWrapper, BorderLayout.CENTER)
            updateDisclosureState()
        }
    }

    override fun validate(): com.intellij.openapi.ui.ValidationInfo? = validateActonExecutable()

    override fun buildUI(settingsStep: SettingsStep) {
        settingsStep.addSettingsComponent(component)
    }

    companion object {
        private val LICENSE_OPTIONS = listOf(
            "MIT",
            "Apache-2.0",
            "GPL-3.0",
            "BSD-3-Clause",
            "ISC",
            "Unlicense",
        )
    }
}
