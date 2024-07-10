package org.ton.intellij.func.ide.configurable

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.panel
import org.ton.intellij.func.FuncBundle
import org.ton.intellij.func.FuncLanguageLevel
import org.ton.intellij.func.ide.settings.funcSettings

class FuncProjectConfigurable(
    val project: Project,
) : BoundConfigurable(FuncBundle.message("func.name")) {
    override fun createPanel(): DialogPanel = panel {
        val settings = project.funcSettings
        val state = settings.state.copy()

        row {
            comboBox(EnumComboBoxModel(FuncLanguageLevel::class.java).apply {
                this.setSelectedItem(state.languageLevel)
            }, SimpleListCellRenderer.create("") { it.displayName })
        }

        onApply {
            settings.state.also {
                it.languageLevel = state.languageLevel
            }
        }
    }
}
