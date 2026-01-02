package org.ton.intellij.boc

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import javax.swing.JPanel

class SetupActonActionPanel(
    private val project: Project,
    private val onInstalled: (() -> Unit)? = null,
) : JPanel(BorderLayout()), Disposable {

    init {
        add(buildUi(), BorderLayout.CENTER)
    }

    private fun buildUi() = panel {
        row { label("Acton is not configured").bold() }
        row { text("Acton is required for BOC disassembly and analysis.") }
        row {
            button("Configure Acton") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Acton")
                onInstalled?.invoke()
            }
            link("Open documentation") {
                BrowserUtil.browse("https://i582.github.io/acton/docs/installation/")
            }
        }
        row {
            comment("Please specify the path to the <code>acton</code> executable in the settings.")
        }
    }

    override fun dispose() {}
}
