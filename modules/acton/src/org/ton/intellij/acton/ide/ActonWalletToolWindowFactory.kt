package org.ton.intellij.acton.ide

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionManager
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
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import java.awt.BorderLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.datatransfer.StringSelection
import javax.swing.*

class ActonWalletToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ActonWalletPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class ActonWalletPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val cardsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = JBUI.CurrentTheme.Table.background(false, false)
    }
    private val scrollPane = com.intellij.ui.components.JBScrollPane(cardsPanel).apply {
        border = JBUI.Borders.empty()
        verticalScrollBar.unitIncrement = 16
    }
    private val gson = Gson()

    companion object {
        val WALLET_VERSIONS = listOf(
            "v5r1", "v4r2", "v4r1", "v3r2", "v3r1", "v2r2", "v2r1", "v1r3", "v1r2", "v1r1",
            "highloadv2r2", "highloadv2r1", "highloadv2", "highloadv1r2", "highloadv1r1"
        )
    }

    init {
        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction("Refresh Wallets", "Update wallet list", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) = refreshWallets()
            })
            add(object : AnAction("New Wallet", "Create a new wallet", AllIcons.General.Add) {
                override fun actionPerformed(e: AnActionEvent) = createNewWallet()
            })
            add(object : AnAction("Import Wallet", "Import existing wallet", AllIcons.ToolbarDecorator.Import) {
                override fun actionPerformed(e: AnActionEvent) = importWallet()
            })
        }

        val toolbar = ActionManager.getInstance().createActionToolbar("ActonWalletToolbar", actionGroup, true)
        toolbar.targetComponent = this

        val toolbarPanel = JPanel(BorderLayout())
        toolbarPanel.add(toolbar.component, BorderLayout.WEST)
        toolbarPanel.border = JBUI.Borders.empty()

        add(toolbarPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)

        border = JBUI.Borders.empty()
        refreshWallets()
    }

    private fun refreshWallets() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val projectDir = project.guessProjectDir()
            if (projectDir == null) {
                showError("Could not find project directory")
                return@executeOnPooledThread
            }

            val walletCommand = ActonCommand.Wallet.ListCmd(json = true)
            val commandLine = ActonCommandLine(
                command = walletCommand.name,
                workingDirectory = projectDir.toNioPath(),
                additionalArguments = walletCommand.getArguments(),
                environmentVariables = EnvironmentVariablesData.DEFAULT
            ).toGeneralCommandLine(project)

            val handler = CapturingProcessHandler(commandLine)
            val output = handler.runProcess(10000)

            ApplicationManager.getApplication().invokeLater {
                cardsPanel.removeAll()

                if (output.exitCode != 0) {
                    val error = stripAnsiColors(output.stderr.ifBlank { output.stdout })
                    addStatusLabel("Error: $error")
                } else {
                    try {
                        val info = gson.fromJson(output.stdout, WalletListInfo::class.java)
                        if (info.success) {
                            if (info.wallets.isEmpty()) {
                                addStatusLabel("No wallets found")
                            } else {
                                info.wallets.forEach {
                                    cardsPanel.add(WalletCard(it))
                                    cardsPanel.add(Box.createVerticalStrut(8))
                                }
                                cardsPanel.add(Box.createVerticalGlue())
                            }
                        } else {
                            addStatusLabel("Failed to list wallets")
                        }
                    } catch (e: Exception) {
                        addStatusLabel("Error parsing response: ${e.message}")
                    }
                }

                cardsPanel.revalidate()
                cardsPanel.repaint()
            }
        }
    }

    private fun addStatusLabel(text: String) {
        cardsPanel.add(Box.createVerticalGlue())
        val label = JBLabel(text, SwingConstants.CENTER).apply {
            alignmentX = CENTER_ALIGNMENT
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
        }
        cardsPanel.add(label)
        cardsPanel.add(Box.createVerticalGlue())
    }

    private fun showError(message: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(project, message, "Acton Wallet Error")
        }
    }

    private inner class WalletCard(val info: WalletInfo) : JPanel(BorderLayout()) {
        init {
            isOpaque = false
            border = JBUI.Borders.empty(4)
            alignmentX = LEFT_ALIGNMENT

            val content = object : JPanel(BorderLayout()) {
                override fun paintComponent(g: Graphics) {
                    val g2 = g.create() as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.color = JBUI.CurrentTheme.EditorTabs.background()
                    g2.fillRoundRect(0, 0, width, height, 12, 12)
                    g2.dispose()
                }
            }.apply {
                isOpaque = false
                border = JBUI.Borders.empty(8, 12)

                val mainInfo = panel {
                    row {
                        label(info.name).bold().align(AlignX.LEFT)
                        label(if (info.isGlobal) "Global" else "Local")
                            .applyToComponent {
                                foreground = JBUI.CurrentTheme.Label.disabledForeground()
                                font = JBUI.Fonts.smallFont()
                            }
                            .align(AlignX.RIGHT)
                    }
                    row {
                        val truncatedAddr = if (info.address.length > 20) {
                            info.address.take(8) + "…" + info.address.takeLast(8)
                        } else info.address

                        link(truncatedAddr) {
                            BrowserUtil.browse("https://testnet.tonviewer.com/${info.address}")
                        }.applyToComponent {
                            toolTipText = "Open ${info.address} in Tonviewer"
                        }

                        icon(AllIcons.Actions.Copy).applyToComponent {
                            cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                            toolTipText = "Copy address"
                            val originalIcon = icon
                            addMouseListener(object : java.awt.event.MouseAdapter() {
                                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                                    CopyPasteManager.getInstance().setContents(StringSelection(info.address))
                                    icon = AllIcons.General.InspectionsOK

                                    val timer = Timer(1500) {
                                        icon = originalIcon
                                    }
                                    timer.isRepeats = false
                                    timer.start()
                                }
                            })
                        }
                    }
                    row {
                        label("Type: ${info.kind}").applyToComponent {
                            font = JBUI.Fonts.smallFont()
                            foreground = JBUI.CurrentTheme.Label.disabledForeground()
                        }
                    }
                }.apply {
                    background = JBUI.CurrentTheme.EditorTabs.background()
                    isOpaque = false
                }
                add(mainInfo, BorderLayout.CENTER)
            }

            add(content, BorderLayout.CENTER)
        }

        override fun getMaximumSize(): java.awt.Dimension {
            return java.awt.Dimension(Int.MAX_VALUE, preferredSize.height)
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
            VfsUtil.markDirtyAndRefresh(true, true, true, projectDir)
        }

        val home = System.getProperty("user.home")
        val globalPath = java.nio.file.Paths.get(home, ".acton", "wallets", "global.wallets.toml").toFile()
        LocalFileSystem.getInstance().refreshIoFiles(listOf(globalPath))
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
