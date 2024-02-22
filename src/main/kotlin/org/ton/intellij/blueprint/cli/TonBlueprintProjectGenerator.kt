package org.ton.intellij.blueprint.cli

import com.intellij.execution.filters.Filter
import com.intellij.ide.util.projectWizard.MultiWebTemplateNewProjectWizard
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.ide.util.projectWizard.WebTemplateProjectWizardStep
import com.intellij.ide.wizard.AbstractNewProjectWizardMultiStepBase
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.ide.wizard.NewProjectWizardBaseStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.javascript.nodejs.packages.NodePackageUtil
import com.intellij.lang.javascript.boilerplate.NpmPackageProjectGenerator
import com.intellij.lang.javascript.boilerplate.NpxPackageDescriptor
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.PathUtil
import org.ton.intellij.blueprint.BlueprintIcons
import org.ton.intellij.blueprint.action.InstallBlueprintAction
import java.io.File
import javax.swing.Icon
import javax.swing.JPanel

class TonBlueprintProjectGenerator(
    val projectType: ProjectType
) : NpmPackageProjectGenerator() {

    override fun getId(): String = "ton-blueprint-${projectType.id}"

    @Suppress("DialogTitleCapitalization")
    override fun getName(): String = projectType.displayName

    override fun getDescription(): String =
        "Create a new <a href='https://github.com/ton-org/blueprint'>TON Blueprint</a> project using CLI"

    override fun getIcon(): Icon = BlueprintIcons.BLUEPRINT

    override fun presentablePackageName(): String = "TON"

    override fun filters(p0: Project, p1: VirtualFile): Array<Filter> = emptyArray()

    override fun customizeModule(p0: VirtualFile, p1: ContentEntry?) {
    }

    override fun getNpxCommands(): List<NpxPackageDescriptor.NpxCommand> {
        return listOf(
            NpxPackageDescriptor.NpxCommand(CREATE_TON_PACKAGE_NAME, CREATE_TON_PACKAGE_NAME),
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
        val addSampleCode = settings.getUserData(ADD_SAMPLE_CODE) ?: false
        if (packageName.contains(CREATE_TON_PACKAGE_NAME)) {
            return arrayOf(workingDir, "--type", projectType.argument(addSampleCode), "--contractName", "Main")
        }
        return arrayOf(
            CREATE_COMMAND,
            "--type",
            projectType.argument(addSampleCode),
            "--contractName",
            "Main",
            workingDir
        )
    }

    override fun postInstall(project: Project, baseDir: VirtualFile, workingDir: File?): Runnable = Runnable {
        super.postInstall(project, baseDir, workingDir)

        val packageJson = PackageJsonUtil.findChildPackageJsonFile(baseDir)
        if (packageJson != null) {
            val action = InstallBlueprintAction(project, packageJson)
            val event = AnActionEvent.createFromDataContext("Dummy", null, DataContext.EMPTY_CONTEXT)
            action.actionPerformed(event)
        }
    }

    override fun createPeer(): ProjectGeneratorPeer<Settings> {
        return object : NpmPackageGeneratorPeer() {
            private lateinit var sampleCode: JBCheckBox
            private var newCamelCaseCodeStyle: JBCheckBox? = null

            override fun createPanel(): JPanel {
                return super.createPanel().apply {
                    sampleCode = JBCheckBox("Add sample code").also {
                        add(it)
                    }
                    if (projectType == ProjectType.FUNC) {
                        newCamelCaseCodeStyle = JBCheckBox("Use new camelCase code style").also {
                            add(it)
                        }
                    }
                }
            }

            override fun buildUI(settingsStep: SettingsStep) {
                super.buildUI(settingsStep)
                settingsStep.addSettingsComponent(sampleCode)
                newCamelCaseCodeStyle?.let { settingsStep.addSettingsComponent(it) }
            }

            override fun getSettings(): Settings {
                return super.getSettings().apply {
                    putUserData(ADD_SAMPLE_CODE, sampleCode.isSelected)
                    putUserData(ADD_NEW_CAMEL_CASE_CODE_STYLE, newCamelCaseCodeStyle?.isSelected ?: false)
                }
            }
        }
    }

    override fun generateInTemp(): Boolean = false

    companion object {
        private val ADD_SAMPLE_CODE = Key.create<Boolean>("create.ton.blueprint.add_sample_code")
        private val ADD_NEW_CAMEL_CASE_CODE_STYLE =
            Key.create<Boolean>("create.ton.blueprint.func.new_camel_case_code_style")

        const val CREATE_TON_PACKAGE_NAME = "create-ton"
        const val CREATE_COMMAND = "create"
    }

    enum class ProjectType(
        val id: String,
        val displayName: String,
    ) {
        FUNC("func", "FunC"),
        TACT("tact", "Tact");

        fun argument(addSampleCode: Boolean): String {
            return if (addSampleCode) {
                "$id-counter"
            } else {
                "$id-empty"
            }
        }
    }
}

class TonBlueprintProjectWizard : MultiWebTemplateNewProjectWizard(
    listOf(
        TonBlueprintProjectGenerator(TonBlueprintProjectGenerator.ProjectType.FUNC),
        TonBlueprintProjectGenerator(TonBlueprintProjectGenerator.ProjectType.TACT)
    )
) {
    override val icon: Icon
        get() = BlueprintIcons.TON_SYMBOL
    override val id: String
        get() = "ton-blueprint"
    override val name: String
        get() = "TON"

    override fun createTemplateStep(parent: NewProjectWizardBaseStep): NewProjectWizardStep {
        return object : AbstractNewProjectWizardMultiStepBase(parent) {
            override val label: String
                get() = UIBundle.message("label.project.wizard.new.project.language")

            override fun initSteps() = templates.associateBy({ it.name }, { WebTemplateProjectWizardStep(parent, it) })
        }
    }
}

class TonBlueprintProjectModuleBuilder : GeneratorNewProjectWizardBuilderAdapter(TonBlueprintProjectWizard()) {
    override fun getWeight(): Int {
        return 10
    }
}
