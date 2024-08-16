package org.ton.intellij.tolk.ide.configurable

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.panel
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.TolkLanguageLevel
import org.ton.intellij.tolk.ide.settings.funcSettings

class TolkProjectConfigurable(
    val project: Project,
) : BoundConfigurable(TolkBundle.message("tolk.name")) {
    override fun createPanel(): DialogPanel = panel {
        val settings = project.funcSettings
        val state = settings.state.copy()

        row(TolkBundle.message("tolk.language.level")) {
            comboBox(EnumComboBoxModel(TolkLanguageLevel::class.java).apply {
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
