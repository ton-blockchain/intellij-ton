package org.ton.intellij.acton.ide

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.PopupHandler
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import javax.swing.*
import javax.swing.table.DefaultTableModel

class ActonWalletToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ActonWalletPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class ActonWalletPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val tableModel = createTableModel()
    private val table = JBTable(tableModel)
    private val gson = Gson()

    companion object {
        val WALLET_VERSIONS = listOf(
            "v5r1", "v4r2", "v4r1", "v3r2", "v3r1", "v2r2", "v2r1", "v1r3", "v1r2", "v1r1",
            "highloadv2r2", "highloadv2r1", "highloadv2", "highloadv1r2", "highloadv1r1"
        )
    }

    init {
        setupTable(table)

        val decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction { createNewWallet() }
            .setAddActionName("New Wallet")
            .addExtraAction(object : AnAction("Import Wallet", null, AllIcons.ToolbarDecorator.Import) {
                override fun actionPerformed(e: AnActionEvent) = importWallet()
            })
            .addExtraAction(object : AnAction("Refresh Wallets", null, AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) = refreshWallets()
            })
            .disableRemoveAction()
            .disableUpDownActions()

        add(decorator.createPanel().apply {
            border = JBUI.Borders.empty()
        }, BorderLayout.CENTER)

        border = JBUI.Borders.empty()
        refreshWallets()
    }

    private fun createTableModel() = object : DefaultTableModel(arrayOf("Name", "Address", "Type", "Scope"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
    }

    private fun setupTable(table: JBTable) {
        val copyAction = object : AnAction("Copy Address", null, AllIcons.Actions.Copy) {
            override fun actionPerformed(e: AnActionEvent) {
                val row = table.selectedRow
                if (row != -1) {
                    val address = table.getValueAt(row, 1) as String
                    CopyPasteManager.getInstance().setContents(StringSelection(address))
                }
            }
        }

        val group = DefaultActionGroup()
        group.add(copyAction)

        table.addMouseListener(object : PopupHandler() {
            override fun invokePopup(comp: java.awt.Component, x: Int, y: Int) {
                val menu = com.intellij.openapi.actionSystem.ActionManager.getInstance()
                    .createActionPopupMenu("ActonWalletPopup", group)
                menu.component.show(comp, x, y)
            }
        })
    }

    private fun refreshWallets() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val walletCommand = ActonCommand.Wallet.ListCmd(json = true)
            val commandLine = ActonCommandLine(
                command = walletCommand.name,
                workingDirectory = project.guessProjectDir()?.toNioPath() ?: return@executeOnPooledThread,
                additionalArguments = walletCommand.getArguments(),
                environmentVariables = EnvironmentVariablesData.DEFAULT
            ).toGeneralCommandLine(project)

            val handler = CapturingProcessHandler(commandLine)
            val output = handler.runProcess(10000)
            if (output.exitCode == 0) {
                val info = gson.fromJson(output.stdout, WalletListInfo::class.java)
                if (info.success) {
                    ApplicationManager.getApplication().invokeLater {
                        tableModel.rowCount = 0
                        info.wallets.forEach {
                            tableModel.addRow(arrayOf(it.name, it.address, it.kind, if (it.isGlobal) "Global" else "Local"))
                        }
                    }
                }
            }
        }
    }

    private fun createNewWallet() {
        val dialog = NewWalletDialog(project)
        if (dialog.showAndGet()) {
            val name = dialog.walletName
            val version = dialog.version ?: "v5r1"
            val global = dialog.isGlobal
            val secure = dialog.isSecure

            ApplicationManager.getApplication().executeOnPooledThread {
                val walletCommand = ActonCommand.Wallet.New(
                    walletName = name,
                    version = version,
                    global = global,
                    local = !global,
                    secure = secure,
                    json = true
                )
                val commandLine = ActonCommandLine(
                    command = walletCommand.name,
                    workingDirectory = project.guessProjectDir()?.toNioPath() ?: return@executeOnPooledThread,
                    additionalArguments = walletCommand.getArguments(),
                    environmentVariables = EnvironmentVariablesData.DEFAULT
                ).toGeneralCommandLine(project)

                val handler = CapturingProcessHandler(commandLine)
                val output = handler.runProcess(30000)
                ApplicationManager.getApplication().invokeLater {
                    if (output.exitCode == 0) {
                        refreshVfs()
                        refreshWallets()
                    } else {
                        val errorMessage = stripAnsiColors(output.stderr.ifBlank { output.stdout })
                        Messages.showErrorDialog(project, errorMessage, "Error Creating Wallet")
                    }
                }
            }
        }
    }

    private fun importWallet() {
        val dialog = ImportWalletDialog(project)
        if (dialog.showAndGet()) {
            val name = dialog.walletName
            val mnemonic = dialog.mnemonic
            val version = dialog.version ?: "v5r1"
            val global = dialog.isGlobal
            val secure = dialog.isSecure

            ApplicationManager.getApplication().executeOnPooledThread {
                val walletCommand = ActonCommand.Wallet.Import(
                    walletName = name,
                    mnemonics = listOf(mnemonic),
                    version = version,
                    global = global,
                    local = !global,
                    secure = secure,
                    json = true
                )
                val commandLine = ActonCommandLine(
                    command = walletCommand.name,
                    workingDirectory = project.guessProjectDir()?.toNioPath() ?: return@executeOnPooledThread,
                    additionalArguments = walletCommand.getArguments(),
                    environmentVariables = EnvironmentVariablesData.DEFAULT
                ).toGeneralCommandLine(project)

                val handler = CapturingProcessHandler(commandLine)
                val output = handler.runProcess(30000)
                ApplicationManager.getApplication().invokeLater {
                    if (output.exitCode == 0) {
                        refreshVfs()
                        refreshWallets()
                    } else {
                        val errorMessage = stripAnsiColors(output.stderr.ifBlank { output.stdout })
                        Messages.showErrorDialog(project, errorMessage, "Error Importing Wallet")
                    }
                }
            }
        }
    }

    private data class WalletListInfo(val success: Boolean, val wallets: List<WalletInfo>)
    private data class WalletInfo(
        val name: String,
        val address: String,
        val kind: String,
        @SerializedName("is_global") val isGlobal: Boolean,
    )

    private fun stripAnsiColors(text: String): String {
        return org.ton.intellij.acton.ActonUtils.stripAnsiColors(text)
    }

    private fun refreshVfs() {
        val projectDir = project.guessProjectDir()
        if (projectDir != null) {
            projectDir.findChild("wallets.toml")?.refresh(false, false)
            projectDir.findChild("global.wallets.toml")?.refresh(false, false)
        }

        val home = System.getProperty("user.home")
        val globalPath = java.nio.file.Paths.get(home, ".acton", "wallets", "global.wallets.toml")
        LocalFileSystem.getInstance().findFileByNioFile(globalPath)?.refresh(false, false)
    }
}

class NewWalletDialog(project: Project) : DialogWrapper(project) {
    var walletName: String = ""
    var version: String? = "v5r1"
    var isGlobal: Boolean = false
    var isSecure: Boolean = true
    private lateinit var panel: com.intellij.openapi.ui.DialogPanel

    init {
        title = "New Wallet"
        init()
    }

    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Name:") {
                textField()
                    .bindText(::walletName)
                    .align(AlignX.FILL)
            }
            row("Type:") {
                comboBox(ActonWalletPanel.WALLET_VERSIONS)
                    .bindItem(::version)
                    .align(AlignX.FILL)
            }
            row {
                checkBox("Store in global wallets")
                    .bindSelected(::isGlobal)
            }
            row {
                checkBox("Use secure native store")
                    .bindSelected(::isSecure)
            }
        }
        return panel
    }

    override fun doOKAction() {
        panel.apply()
        super.doOKAction()
    }
}

class ImportWalletDialog(project: Project) : DialogWrapper(project) {
    var walletName: String = ""
    var mnemonic: String = ""
    var version: String? = "v5r1"
    var isGlobal: Boolean = false
    var isSecure: Boolean = true
    private lateinit var panel: com.intellij.openapi.ui.DialogPanel

    init {
        title = "Import Wallet"
        init()
    }

    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Name:") {
                textField()
                    .bindText(::walletName)
                    .align(AlignX.FILL)
            }
            row("Type:") {
                comboBox(ActonWalletPanel.WALLET_VERSIONS)
                    .bindItem(::version)
                    .align(AlignX.FILL)
            }
            row("Mnemonic:") {
                textArea()
                    .bindText(::mnemonic)
                    .align(AlignX.FILL)
                    .rows(3)
                    .comment("Enter 24 words separated by spaces")
            }
            row {
                checkBox("Store in global wallets")
                    .bindSelected(::isGlobal)
            }
            row {
                checkBox("Use secure native store")
                    .bindSelected(::isSecure)
            }
        }
        return panel
    }

    override fun doOKAction() {
        panel.apply()
        super.doOKAction()
    }
}
