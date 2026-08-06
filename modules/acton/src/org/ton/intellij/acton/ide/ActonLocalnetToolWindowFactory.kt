package org.ton.intellij.acton.ide

import com.google.gson.Gson
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ScalableIcon
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.cli.ActonToml
import java.awt.AlphaComposite
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridLayout
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.Timer
import javax.swing.UIManager

class ActonLocalnetToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ActonLocalnetPanel(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class ActonLocalnetPanel(private val project: Project, private val toolWindow: ToolWindow) : JPanel(BorderLayout()) {
    private val service = project.actonLocalnetService
    private val gson = Gson()

    private val statusValue = JBLabel()
    private val statusDotIcon = StatusDotIcon()
    private val ownershipValue = OwnershipBadgeLabel()
    private val portValue = JBLabel()
    private val lastBlockSeqnoValue = JBLabel()
    private val modeValue = JBLabel()
    private val forkSourceValue = JBLabel()
    private val uptimeValue = JBLabel()
    private val accountsValue = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        alignmentX = Component.LEFT_ALIGNMENT
    }
    private val refreshTimer = Timer(1000) { refreshState() }
    private var walletAddressesByName: Map<String, String> = emptyMap()
    private var walletRefreshInFlight: Boolean = false
    private var walletRefreshKey: Pair<Path, List<String>>? = null

    init {
        configureStaticAppearance()

        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction("Refresh", "Refresh localnet state", AllIcons.Actions.Refresh) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

                override fun actionPerformed(e: AnActionEvent) = refreshState(force = true)
            })
            add(ToggleLocalnetAction())
            add(RestartLocalnetAction())
            add(ShowLogAction())
            add(AirdropLocalnetAction())
            addSeparator()
            add(DumpStateAction())
            add(LoadStateAction())
            add(ClearDbAction())
            addSeparator()
            add(OpenLocalnetTargetAction("Open UI", "Open localnet UI", AllIcons.Actions.Preview) { service.openUi() })
        }

        val toolbar = ActionManager.getInstance().createActionToolbar("ActonLocalnetToolbar", actionGroup, true)
        toolbar.targetComponent = this

        add(toolbar.component, BorderLayout.NORTH)
        add(buildContent(), BorderLayout.CENTER)

        refreshTimer.start()
        refreshState(force = true)
    }

    override fun removeNotify() {
        refreshTimer.stop()
        super.removeNotify()
    }

    private fun configureStaticAppearance() {
        statusValue.font = statusValue.font.deriveFont(Font.BOLD, statusValue.font.size2D + 9f)
        statusValue.iconTextGap = JBUI.scale(8)
        statusValue.alignmentX = Component.LEFT_ALIGNMENT
        statusValue.icon = statusDotIcon

        ownershipValue.font = ownershipValue.font.deriveFont(Font.BOLD, ownershipValue.font.size2D - 1f)
        ownershipValue.alignmentX = Component.LEFT_ALIGNMENT
        ownershipValue.alignmentY = Component.CENTER_ALIGNMENT

        listOf(portValue, lastBlockSeqnoValue, uptimeValue).forEach {
            it.font = it.font.deriveFont(Font.BOLD, it.font.size2D + 3f)
        }

        listOf(modeValue, forkSourceValue).forEach {
            it.alignmentX = Component.LEFT_ALIGNMENT
        }
    }

    private fun buildContent(): JComponent = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(12)
        isOpaque = false

        add(lockHeight(buildSummaryPanel()))
        add(Box.createVerticalStrut(JBUI.scale(12)))
        add(lockHeight(buildMetricsPanel()))
        add(Box.createVerticalStrut(JBUI.scale(16)))
        add(lockHeight(buildSection("Runtime", listOf("Mode" to modeValue, "Fork source" to forkSourceValue))))
        add(Box.createVerticalStrut(JBUI.scale(12)))
        add(lockHeight(buildSection("Funding", listOf("Startup accounts" to accountsValue))))
        add(Box.createVerticalGlue())
    }

    private fun buildSummaryPanel(): JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = false
        alignmentX = Component.LEFT_ALIGNMENT

        add(statusValue)
        add(Box.createHorizontalGlue())
        add(ownershipValue)
    }

    private fun buildMetricsPanel(): JPanel = JPanel(GridLayout(1, 3, JBUI.scale(8), 0)).apply {
        isOpaque = false
        alignmentX = Component.LEFT_ALIGNMENT

        add(createMetricPanel("Port", portValue))
        add(createMetricPanel("Block", lastBlockSeqnoValue))
        add(createMetricPanel("Uptime", uptimeValue))

        val metricHeight = JBUI.scale(74)
        preferredSize = Dimension(preferredSize.width, metricHeight)
        minimumSize = Dimension(minimumSize.width, metricHeight)
        maximumSize = Dimension(Int.MAX_VALUE, metricHeight)
    }

    private fun createMetricPanel(title: String, valueLabel: JBLabel): JPanel {
        val titleLabel = JBLabel(title).apply {
            foreground = secondaryTextColor()
            font = font.deriveFont(font.size2D - 1f)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tileBorderColor()),
                JBUI.Borders.empty(8, 10),
            )
            add(titleLabel)
            add(Box.createVerticalStrut(JBUI.scale(6)))
            add(valueLabel)

            val metricHeight = JBUI.scale(74)
            preferredSize = Dimension(preferredSize.width, metricHeight)
            minimumSize = Dimension(minimumSize.width, metricHeight)
            maximumSize = Dimension(Int.MAX_VALUE, metricHeight)
        }
    }

    private fun buildSection(title: String, fields: List<Pair<String, JComponent>>): JPanel {
        val titleLabel = JBLabel(title.uppercase()).apply {
            foreground = secondaryTextColor()
            font = font.deriveFont(Font.BOLD, font.size2D - 1f)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT

            add(titleLabel)
            add(Box.createVerticalStrut(JBUI.scale(8)))
            fields.forEachIndexed { index, (label, value) ->
                add(createFieldPanel(label, value))
                if (index != fields.lastIndex) {
                    add(Box.createVerticalStrut(JBUI.scale(10)))
                }
            }
        }
    }

    private fun createFieldPanel(title: String, valueLabel: JComponent): JPanel {
        val titleLabel = JBLabel(title).apply {
            foreground = secondaryTextColor()
            font = font.deriveFont(font.size2D - 1f)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT

            add(titleLabel)
            add(Box.createVerticalStrut(JBUI.scale(2)))
            add(valueLabel)
        }
    }

    private fun refreshState(force: Boolean = false) {
        service.requestHealthRefresh(force = force)
        val snapshot = service.snapshot()
        val config = ReadAction.compute<LocalnetUiConfig, RuntimeException> {
            val localnet = ActonToml.find(project)?.getLocalnetSettings()
            val workingDir = ActonToml.find(project)?.workingDir
            LocalnetUiConfig(
                workingDir = workingDir,
                port = localnet?.port,
                accounts = localnet?.accounts.orEmpty(),
                mode = localnet.presentableMode(),
                forkSource = localnet.presentableForkSource(),
            )
        }
        requestStartupAccountRefresh(config)

        statusValue.text = snapshot.status.presentableName()
        statusValue.foreground = statusColor(snapshot.status)
        statusDotIcon.dotColor = statusColor(snapshot.status)
        statusDotIcon.showHalo = snapshot.status == LocalnetStatus.RUNNING
        ownershipValue.text = snapshot.ownership.presentableName()
        ownershipValue.foregroundColor = secondaryTextColor()
        toolWindow.setIcon(
            if (snapshot.status.isActive()) {
                ACTIVE_LOCALNET_TOOL_WINDOW_ICON
            } else {
                INACTIVE_LOCALNET_TOOL_WINDOW_ICON
            },
        )
        portValue.text = (if (snapshot.status.isActive()) snapshot.port else (config.port ?: snapshot.port)).toString()
        modeValue.text = snapshot.mode ?: config.mode
        forkSourceValue.text = snapshot.forkSource ?: config.forkSource ?: "Not forked"
        lastBlockSeqnoValue.text = snapshot.lastBlockSeqno?.toString() ?: "Waiting"
        uptimeValue.text = snapshot.uptimeSeconds?.let(::formatDuration) ?: "Waiting"
        renderStartupAccounts(config.accounts, snapshot.port, snapshot.status.isActive())
    }

    private fun requestStartupAccountRefresh(config: LocalnetUiConfig) {
        val workingDir = config.workingDir ?: return
        if (config.accounts.isEmpty()) {
            walletAddressesByName = emptyMap()
            walletRefreshKey = workingDir to emptyList()
            return
        }

        val key = workingDir to config.accounts
        val missingAddress = config.accounts.any { walletAddressesByName[it] == null }
        if (!missingAddress && walletRefreshKey == key) return
        if (walletRefreshInFlight && walletRefreshKey == key) return

        walletRefreshInFlight = true
        walletRefreshKey = key
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            val addresses = fetchWalletAddresses(workingDir)
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                walletAddressesByName = addresses
                walletRefreshInFlight = false
                refreshState()
            }
        }
    }

    private fun fetchWalletAddresses(workingDir: Path): Map<String, String> {
        val walletCommand = ActonCommand.Wallet.ListCmd(balance = false, json = true)
        val commandLine = ActonCommandLine(
            command = walletCommand.name,
            workingDirectory = workingDir,
            additionalArguments = walletCommand.getArguments(),
            environmentVariables = EnvironmentVariablesData.DEFAULT,
        ).toGeneralCommandLine(project) ?: return emptyMap()

        return runCatching {
            val output = CapturingProcessHandler(commandLine).runProcess(10_000)
            if (output.exitCode != 0) return emptyMap()
            val info = gson.fromJson(output.stdout, LocalnetWalletListInfo::class.java)
            if (!info.success) return emptyMap()
            info.wallets.associate { it.name to it.address }
        }.getOrDefault(emptyMap())
    }

    private fun renderStartupAccounts(accountNames: List<String>, localnetPort: Int, isActive: Boolean) {
        accountsValue.removeAll()

        if (accountNames.isEmpty()) {
            accountsValue.add(createPlainStartupAccountLabel("No startup accounts"))
        } else {
            accountNames.forEachIndexed { index, accountName ->
                accountsValue.add(createStartupAccountRow(accountName, localnetPort, isActive))
                if (index != accountNames.lastIndex) {
                    accountsValue.add(Box.createVerticalStrut(JBUI.scale(4)))
                }
            }
        }

        accountsValue.revalidate()
        accountsValue.repaint()
    }

    private fun createStartupAccountRow(accountName: String, localnetPort: Int, isActive: Boolean): JComponent {
        val address = walletAddressesByName[accountName]
        if (address == null || !isActive) {
            return createPlainStartupAccountLabel(accountName)
        }

        val url = ActonLocalnetService.explorerAddressUrl(localnetPort, address)
        val openAction = { BrowserUtil.browse(url) }
        val link = ActionLink(accountName) {
            openAction()
        }.apply {
            toolTipText = "Open $accountName in localnet explorer"
            iconTextGap = JBUI.scale(4)
        }
        val icon = JBLabel(AllIcons.Ide.External_link_arrow).apply {
            foreground = secondaryTextColor()
            border = JBUI.Borders.emptyLeft(4)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = "Open $accountName in localnet explorer"
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    openAction()
                }
            })
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(link)
            add(icon)
        }
    }

    private fun createPlainStartupAccountLabel(text: String): JBLabel = JBLabel(text).apply {
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private fun secondaryTextColor(): Color = JBColor(Color(0x66, 0x6D, 0x75), Color(0x97, 0x9E, 0xA8))

    private fun tileBorderColor(): Color = JBColor(Color(0xD7, 0xDB, 0xE0), Color(0x49, 0x4F, 0x57))

    private fun statusColor(status: LocalnetStatus): Color = when (status) {
        LocalnetStatus.RUNNING -> JBColor(Color(0x2F, 0x7D, 0x32), Color(0x6F, 0xCF, 0x70))
        LocalnetStatus.STARTING, LocalnetStatus.STOPPING ->
            JBColor(Color(0xB2, 0x6A, 0x00), Color(0xFF, 0xB7, 0x4D))
        LocalnetStatus.STOPPED -> UIManager.getColor("Label.foreground") ?: JBColor.BLACK
    }

    private fun <T : JComponent> lockHeight(component: T): T = component.apply {
        val preferred = preferredSize
        maximumSize = Dimension(Int.MAX_VALUE, preferred.height)
    }

    private inner class ToggleLocalnetAction : AnAction() {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            val snapshot = service.snapshot()
            when {
                snapshot.isExternalActive() -> {
                    e.presentation.text = "External Localnet Running"
                    e.presentation.description = "This localnet was not started by the IDE"
                    e.presentation.icon = AllIcons.RunConfigurations.Web_app
                    e.presentation.isEnabled = false
                }
                snapshot.status == LocalnetStatus.STOPPING -> {
                    e.presentation.text = "Stopping Localnet"
                    e.presentation.description = "Wait for localnet to stop"
                    e.presentation.icon = AllIcons.Actions.Suspend
                    e.presentation.isEnabled = false
                }
                snapshot.isManagedActive() -> {
                    e.presentation.text = "Stop Localnet"
                    e.presentation.description = "Stop managed localnet process"
                    e.presentation.icon = AllIcons.Actions.Suspend
                    e.presentation.isEnabled = true
                }
                else -> {
                    e.presentation.text = "Start Localnet"
                    e.presentation.description = "Start Acton localnet"
                    e.presentation.icon = AllIcons.Actions.Execute
                    e.presentation.isEnabled = true
                }
            }
        }

        override fun actionPerformed(e: AnActionEvent) {
            val snapshot = service.snapshot()
            when {
                snapshot.isManagedActive() -> service.stop()
                snapshot.isExternalActive() || snapshot.status == LocalnetStatus.STOPPING -> Unit
                else -> service.startFromProject()
            }
        }
    }

    private inner class RestartLocalnetAction :
        AnAction("Restart Localnet", "Restart managed localnet process", AllIcons.Actions.Restart) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            val snapshot = service.snapshot()
            e.presentation.isEnabled = snapshot.isManagedActive()
        }

        override fun actionPerformed(e: AnActionEvent) {
            service.restartFromProject()
        }
    }

    private inner class ShowLogAction :
        AnAction(
            "Show Log",
            "Show localnet log in the Run tool window",
            AllIcons.Actions.Preview,
        ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = service.snapshot().hasRunContent
        }

        override fun actionPerformed(e: AnActionEvent) {
            service.showLog()
        }
    }

    private inner class AirdropLocalnetAction :
        AnAction(
            "Airdrop",
            "Airdrop TON to an address through localnet faucet",
            AllIcons.Nodes.Deploy,
        ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = service.snapshot().status.isActive()
        }

        override fun actionPerformed(e: AnActionEvent) {
            service.promptAndAirdropFromProject()
        }
    }

    private inner class DumpStateAction :
        AnAction(
            "Dump State",
            "Write current localnet state to a snapshot file",
            AllIcons.ToolbarDecorator.Export,
        ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = service.snapshot().status.isActive()
        }

        override fun actionPerformed(e: AnActionEvent) {
            service.promptAndDumpStateFromProject()
        }
    }

    private inner class LoadStateAction :
        AnAction(
            "Load State",
            "Load localnet state from a snapshot file",
            AllIcons.ToolbarDecorator.Import,
        ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = service.snapshot().status.isActive()
        }

        override fun actionPerformed(e: AnActionEvent) {
            service.promptAndLoadStateFromProject()
        }
    }

    private inner class ClearDbAction :
        AnAction(
            "Clear DB",
            "Delete localnet database files by path",
            AllIcons.General.Remove,
        ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            val snapshot = service.snapshot()
            e.presentation.isEnabled = !snapshot.isExternalActive() && snapshot.status != LocalnetStatus.STOPPING
        }

        override fun actionPerformed(e: AnActionEvent) {
            service.promptAndClearDbFromProject()
        }
    }

    private inner class OpenLocalnetTargetAction(
        text: String,
        description: String,
        icon: Icon,
        private val openAction: () -> Unit,
    ) : AnAction(text, description, icon) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = service.snapshot().status.isActive()
        }

        override fun actionPerformed(e: AnActionEvent) {
            openAction()
        }
    }

    private fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return buildString {
            if (hours > 0) append("${hours}h ")
            if (hours > 0 || minutes > 0) append("${minutes}m ")
            append("${seconds}s")
        }.trim()
    }
}

private val INACTIVE_LOCALNET_TOOL_WINDOW_ICON: Icon = AllIcons.RunConfigurations.Web_app
private val ACTIVE_LOCALNET_TOOL_WINDOW_ICON: Icon = StatusBadgeIcon(
    baseIcon = AllIcons.RunConfigurations.Web_app,
    badgeColor = JBColor(Color(0x2E, 0xC2, 0x4D), Color(0x57, 0xD9, 0x74)),
)

private class StatusDotIcon : Icon {
    var dotColor: Color = JBColor(Color(0x2F, 0x7D, 0x32), Color(0x6F, 0xCF, 0x70))
    var showHalo: Boolean = false

    override fun paintIcon(c: java.awt.Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            if (showHalo) {
                g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f)
                g2.color = dotColor
                g2.fillOval(x, y, iconWidth, iconHeight)
                g2.composite = AlphaComposite.SrcOver
            }
            val dotSize = JBUI.scale(8)
            val dotOffsetX = x + (iconWidth - dotSize) / 2
            val dotOffsetY = y + (iconHeight - dotSize) / 2
            g2.color = dotColor
            g2.fillOval(dotOffsetX, dotOffsetY, dotSize, dotSize)
        } finally {
            g2.dispose()
        }
    }

    override fun getIconWidth(): Int = JBUI.scale(14)

    override fun getIconHeight(): Int = JBUI.scale(14)
}

private class OwnershipBadgeLabel : JBLabel() {
    var foregroundColor: Color = JBColor.BLACK

    override fun paintComponent(g: Graphics) {
        foreground = foregroundColor
        super.paintComponent(g)
    }

    override fun getInsets(): Insets = JBUI.insets(4, 9)
}

private data class LocalnetWalletListInfo(val success: Boolean, val wallets: List<LocalnetWalletInfo>)

private data class LocalnetWalletInfo(val name: String, val address: String)

private class StatusBadgeIcon(private val baseIcon: Icon, private val badgeColor: Color) :
    Icon,
    ScalableIcon {
    override fun paintIcon(c: java.awt.Component?, g: Graphics, x: Int, y: Int) {
        baseIcon.paintIcon(c, g, x, y)

        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val size = 7
            val offsetX = x + iconWidth - size
            val offsetY = y + iconHeight - size

            g2.color = JBColor.WHITE
            g2.fillOval(offsetX - 1, offsetY - 1, size + 2, size + 2)
            g2.color = badgeColor
            g2.fillOval(offsetX, offsetY, size, size)
        } finally {
            g2.dispose()
        }
    }

    override fun getIconWidth(): Int = baseIcon.iconWidth

    override fun getIconHeight(): Int = baseIcon.iconHeight

    override fun getScale(): Float = (baseIcon as? ScalableIcon)?.scale ?: 1.0f

    override fun scale(scaleFactor: Float): Icon {
        val scaledBaseIcon = (baseIcon as? ScalableIcon)?.scale(scaleFactor) ?: baseIcon
        return StatusBadgeIcon(scaledBaseIcon, badgeColor)
    }
}

private data class LocalnetUiConfig(
    val workingDir: Path?,
    val port: Int?,
    val accounts: List<String>,
    val mode: String,
    val forkSource: String?,
)
