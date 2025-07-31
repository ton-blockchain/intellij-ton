package org.ton.intellij.blueprint.cli

import com.intellij.execution.RunManager
import com.intellij.execution.filters.Filter
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.ide.util.projectWizard.WebTemplateNewProjectWizardBase
import com.intellij.ide.util.projectWizard.WebTemplateProjectWizardStep
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.ide.wizard.NewProjectWizardBaseStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.javascript.nodejs.npm.InstallNodeLocalDependenciesAction
import com.intellij.javascript.nodejs.packages.NodePackageUtil
import com.intellij.lang.javascript.boilerplate.NpmPackageProjectGenerator
import com.intellij.lang.javascript.boilerplate.NpxPackageDescriptor
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunConfigurationBuilder
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.PathUtil
import org.ton.intellij.blueprint.BlueprintIcons
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.toolchain.TolkToolchain
import java.io.File
import javax.swing.Icon

class TonBlueprintProjectGenerator() : NpmPackageProjectGenerator() {
    override fun getId(): String = "create-ton"

    override fun getName(): String = "TON"

    override fun getDescription(): String =
        "Create a new <a href='https://github.com/ton-org/blueprint'>TON Blueprint</a> project using CLI"

    override fun getIcon(): Icon = BlueprintIcons.TON_SYMBOL

    override fun presentablePackageName(): String = "create-ton"

    override fun filters(project: Project, baseDir: VirtualFile): Array<Filter> = emptyArray()

    override fun customizeModule(baseDir: VirtualFile, entry: ContentEntry?) {
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

    override fun generatorArgs(project: Project, dir: VirtualFile, settings: Settings): Array<String> {
        val workingDir = if (generateInTemp()) dir.name else "."
        val packageName = settings.myPackage.name
        val addSampleCode = settings.getUserData(ADD_SAMPLE_CODE) ?: false
        val projectType = settings.getUserData(LANGUAGE) ?: ProjectType.TOLK
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

    override fun postInstall(project: Project, baseDir: VirtualFile, workingDir: File): Runnable = Runnable {
        super.postInstall(project, baseDir, workingDir).run()
        val tolkToolchain = TolkToolchain.suggest(project)
        if (tolkToolchain != null) {
            project.tolkSettings.toolchain = tolkToolchain
        }
    }

    override fun configureProject(project: Project, baseDir: VirtualFile) {
        val packageJson = PackageJsonUtil.findChildPackageJsonFile(baseDir) ?: return
        createRunConfigurations(project, baseDir)
        InstallNodeLocalDependenciesAction.runAndShowConsole(project, packageJson)
    }

    private fun createRunConfigurations(project: Project, baseDir: VirtualFile) {
        val packageJson = PackageJsonUtil.findChildPackageJsonFile(baseDir) ?: return
        val build = NpmRunConfigurationBuilder(project)
            .createRunConfiguration(
                "Build", baseDir, packageJson.path, mapOf(
                    "run-script" to "build"
                )
            )
        NpmRunConfigurationBuilder(project)
            .createRunConfiguration(
                "Test", baseDir, packageJson.path, mapOf(
                    "run-script" to "test"
                )
            )
        NpmRunConfigurationBuilder(project)
            .createRunConfiguration(
                "Deploy", baseDir, packageJson.path, mapOf(
                    "run-script" to "start"
                )
            )
        RunManager.getInstance(project).selectedConfiguration = build
    }


    override fun createPeer(): ProjectGeneratorPeer<Settings> {
        return object : NpmPackageGeneratorPeer() {
            private val propertyGraph = PropertyGraph()
            private val generateSampleCode = propertyGraph.property(true)
            private val language = propertyGraph.property(ProjectType.TOLK)

            private val sampleCode = panel {
                row {
                    checkBox("Add sample code").bindSelected(generateSampleCode)
                }
            }
            private val languageButton = panel {
                row {
                    segmentedButton(listOf(ProjectType.TOLK, ProjectType.FUNC)) { text = it.displayName }
                        .bind(language).gap(RightGap.SMALL)
                }.topGap(TopGap.NONE)
            }

            override fun buildUI(settingsStep: SettingsStep) {
                super.buildUI(settingsStep)
                settingsStep.addSettingsField("Language", languageButton)
                settingsStep.addSettingsComponent(sampleCode)
            }

            override fun getSettings(): Settings {
                return super.getSettings().apply {
                    putUserData(ADD_SAMPLE_CODE, generateSampleCode.get())
                    putUserData(LANGUAGE, language.get())
                }
            }
        }
    }

    override fun generateInTemp(): Boolean = false

    companion object {
        private val ADD_SAMPLE_CODE = Key.create<Boolean>("create.ton.blueprint.add_sample_code")
        private val LANGUAGE = Key.create<ProjectType>("create.ton.blueprint.language")

        const val CREATE_TON_PACKAGE_NAME = "create-ton"
        const val CREATE_COMMAND = "create"
    }

    enum class ProjectType(
        val id: String,
        val displayName: String,
    ) {
        TOLK("tolk", "Tolk"),
        FUNC("func", "FunC"),
        ;

        fun argument(addSampleCode: Boolean): String {
            return if (addSampleCode) {
                "$id-counter"
            } else {
                "$id-empty"
            }
        }
    }
}

class TonBlueprintProjectWizard : WebTemplateNewProjectWizardBase() {
    private val template = TonBlueprintProjectGenerator()

    override val id: String get() = template.id
    override val name: String get() = template.name
    override val icon: Icon get() = template.icon

    override fun createTemplateStep(parent: NewProjectWizardBaseStep): NewProjectWizardStep =
        WebTemplateProjectWizardStep(parent, template)
}

class TonBlueprintProjectModuleBuilder : GeneratorNewProjectWizardBuilderAdapter(TonBlueprintProjectWizard()) {
    override fun getWeight(): Int {
        return 10
    }
}
