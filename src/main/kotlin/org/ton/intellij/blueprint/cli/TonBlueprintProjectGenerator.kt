package org.ton.intellij.blueprint.cli

import com.intellij.execution.filters.Filter
import com.intellij.execution.process.ProcessHandler
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.ide.util.projectWizard.WebTemplateNewProjectWizard
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.javascript.nodejs.packages.NodePackageUtil
import com.intellij.lang.javascript.boilerplate.JavaScriptNewTemplatesFactoryBase
import com.intellij.lang.javascript.boilerplate.NpmPackageProjectGenerator
import com.intellij.lang.javascript.boilerplate.NpxPackageDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.platform.ProjectTemplate
import com.intellij.ui.components.JBList
import com.intellij.util.PathUtil
import org.ton.intellij.blueprint.BlueprintIcons
import javax.swing.Icon
import javax.swing.JPanel

class TonBlueprintProjectGenerator : NpmPackageProjectGenerator() {

    override fun getId(): String = "ton-blueprint"

    @Suppress("DialogTitleCapitalization")
    override fun getName(): String = "TON"

    override fun getDescription(): String =
        "Create a new <a href='https://github.com/ton-org/blueprint'>TON Blueprint</a> project using CLI"

    override fun getIcon(): Icon = BlueprintIcons.BLUEPRINT

    override fun presentablePackageName(): String = "TON"

    override fun filters(p0: Project, p1: VirtualFile): Array<Filter> = emptyArray()

    override fun customizeModule(p0: VirtualFile, p1: ContentEntry?) {
    }

    override fun getNpxCommands(): List<NpxPackageDescriptor.NpxCommand> {
        return listOf(
            NpxPackageDescriptor.NpxCommand(CREATE_TON_PACKAGE_NAME, CREATE_TON_PACKAGE_NAME)
        )
    }

    override fun validateProjectPath(path: String): String? {
        val error = NodePackageUtil.validateNpmPackageName(PathUtil.getFileName(path))
        return error ?: super.validateProjectPath(path)
    }

    override fun packageName(): String = CREATE_TON_PACKAGE_NAME

    override fun generatorArgs(project: Project?, dir: VirtualFile, settings: Settings): Array<String> {
        val workingDir = if (generateInTemp()) dir.name else "."
        val packageName = settings.myPackage.name
        if (packageName.contains(CREATE_TON_PACKAGE_NAME)) {
            return arrayOf(workingDir, "--type", "func-empty")
        }
        return arrayOf(CREATE_COMMAND, "--type", "func-empty", workingDir)
    }

    override fun createPeer(): ProjectGeneratorPeer<Settings> {
        val default = JBList<String>("tact", "func")

        return object : NpmPackageGeneratorPeer() {
            override fun createPanel(): JPanel {
                return super.createPanel()
            }

            override fun buildUI(settingsStep: SettingsStep) {
                super.buildUI(settingsStep)
            }
        }
    }

    override fun onProcessHandlerCreated(processHandler: ProcessHandler) {
//        processHandler.addProcessListener(object : ProcessAdapter() {
//            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
//                if (event.text.contains("Project name")) {
//                    event.processHandler.removeProcessListener(this)
//                    val processInput = event.processHandler.processInput
//                    if (processInput != null) {
//                        try {
//                            processInput.write()
//                        } catch (e: IOException) {
//                            LOG.warn("Failed to write project name to the console.", e)
//                        }
//                    }
//                }
//            }
//        })
    }

    override fun generateInTemp(): Boolean = true

    companion object {
        private val LOG = Logger.getInstance(TonBlueprintProjectGenerator::class.java)

        const val CREATE_TON_PACKAGE_NAME = "create-ton"
        const val CREATE_COMMAND = "create"
    }
}

class TonBlueprintProjectModuleBuilder :
    GeneratorNewProjectWizardBuilderAdapter(WebTemplateNewProjectWizard(TonBlueprintProjectGenerator()))

class TonBlueprintProjectTemplateFactory : JavaScriptNewTemplatesFactoryBase() {
    override fun createTemplates(p0: WizardContext?): Array<ProjectTemplate> =
        arrayOf(TonBlueprintProjectGenerator())
}
