package org.ton.intellij.tlb.settings

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.ton.intellij.tlb.TlbBundle
import javax.swing.JComponent

class TlbSettingsConfigurable(private val project: Project) : Configurable {
    private lateinit var globalBlockTlbField: TextFieldWithBrowseButton

    override fun getDisplayName(): String = TlbBundle.message("tlb.settings.name")

    override fun createComponent(): JComponent {
        globalBlockTlbField = TextFieldWithBrowseButton().apply {
            addActionListener {
                val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("tlb")
                val fileChooser = FileChooser.chooseFile(descriptor, null, null)
                fileChooser?.let { file ->
                    text = file.path
                }
            }
        }

        return panel {
            row {
                label(TlbBundle.message("tlb.settings.global.block.tlb.label"))
                cell(globalBlockTlbField).align(Align.FILL).comment(TlbBundle.message("tlb.settings.global.block.tlb.comment"))
            }
        }
    }

    override fun isModified(): Boolean {
        val settings = TlbSettingsState.getInstance(project)
        return globalBlockTlbField.text != settings.globalBlockTlbPath
    }

    override fun apply() {
        val settings = TlbSettingsState.getInstance(project)
        settings.globalBlockTlbPath = globalBlockTlbField.text
    }

    override fun reset() {
        val settings = TlbSettingsState.getInstance(project)
        globalBlockTlbField.text = settings.globalBlockTlbPath
    }
}
