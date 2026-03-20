package org.ton.intellij.tolk.debug.retrace

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.nio.file.Paths
import javax.swing.JComponent

class TolkRetraceConfigurationEditor(project: Project) : SettingsEditor<TolkRetraceConfiguration>() {
    private lateinit var panel: DialogPanel

    private val transactionHashField = JBTextField()
    private val contractIdField = JBTextField()
    private val networkComboBox = ComboBox(arrayOf("", "testnet", "mainnet"))
    private val workingDirectoryField = TextFieldWithBrowseButton()

    init {
        workingDirectoryField.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Select Working Directory")
        )
    }

    override fun resetEditorFrom(configuration: TolkRetraceConfiguration) {
        transactionHashField.text = configuration.transactionHash
        contractIdField.text = configuration.contractId
        networkComboBox.selectedItem = configuration.network
        workingDirectoryField.text = configuration.workingDirectory?.toString().orEmpty()
        if (::panel.isInitialized) {
            panel.reset()
        }
    }

    override fun applyEditorTo(configuration: TolkRetraceConfiguration) {
        configuration.transactionHash = transactionHashField.text.trim()
        configuration.contractId = contractIdField.text.trim()
        configuration.network = networkComboBox.selectedItem as? String ?: ""
        configuration.workingDirectory = workingDirectoryField.text.takeIf { it.isNotBlank() }?.let(Paths::get)
    }

    override fun createEditor(): JComponent {
        panel = panel {
            row("Transaction hash:") {
                cell(transactionHashField).align(AlignX.FILL)
            }
            row("Contract id:") {
                cell(contractIdField).align(AlignX.FILL)
            }
            row("Network:") {
                cell(networkComboBox).align(AlignX.FILL)
            }
            row("Working directory:") {
                cell(workingDirectoryField).align(AlignX.FILL)
            }
        }
        return panel
    }
}
