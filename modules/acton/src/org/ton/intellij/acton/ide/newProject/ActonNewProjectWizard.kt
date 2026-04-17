package org.ton.intellij.acton.ide.newProject

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.language.LanguageGeneratorNewProjectWizard
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import org.ton.intellij.acton.ActonIcons

class ActonNewProjectWizard : LanguageGeneratorNewProjectWizard {
    override val name: String = "TON"

    override val icon = ActonIcons.TON

    override val ordinal: Int = 900

    override fun createStep(parent: NewProjectWizardStep): NewProjectWizardStep = Step(parent)

    private class Step(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
        private val peer = ActonProjectGeneratorPeer()

        override fun setupUI(builder: Panel) {
            with(builder) {
                row {
                    cell(peer.uiComponent)
                        .align(AlignX.FILL)
                }
            }
        }

        override fun setupProject(project: Project) {
            val builder = ActonModuleBuilder()
            val module = builder.commit(project).firstOrNull() ?: return
            ModuleRootModificationUtil.updateModel(module) { rootModel ->
                builder.configurationData = peer.settings
                builder.setupRootModel(rootModel)
            }
        }
    }
}
