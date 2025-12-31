package org.ton.intellij.acton.runconfig

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.EditorTextField
import com.intellij.ui.ExpandableEditorSupport
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.textCompletion.TextFieldWithCompletion
import com.intellij.util.ui.JBUI
import org.ton.intellij.acton.cli.ActonCommand
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.not
import com.intellij.ui.layout.selected
import java.nio.file.Paths
import javax.swing.JComponent
import com.intellij.openapi.ui.DialogPanel

class ActonCommandConfigurationEditor(private val project: Project) : SettingsEditor<ActonCommandConfiguration>() {
    private lateinit var mainPanel: DialogPanel
    private val commandField = createTextFieldWithCompletion(ActonCommandCompletionProvider())
    private val parametersField = createTextFieldWithCompletion(null)
    private val workingDirectoryField = TextFieldWithBrowseButton()
    private val environmentVariables = EnvironmentVariablesComponent()
    private val emulateTerminal = JBCheckBox("Emulate terminal in output console", true)

    // Build specific
    private val buildContractIdField = createTextFieldWithCompletion(ActonContractCompletionProvider(project))
    private val buildClearCacheCheckBox = JBCheckBox("Clear compilation cache before building", false)
    private val buildOutDirField = TextFieldWithBrowseButton()

    // Script specific
    private val scriptPathField = TextFieldWithBrowseButton()
    private val scriptClearCacheCheckBox = JBCheckBox("Clear compilation cache before running", false)
    private val scriptForkCheckBox = JBCheckBox("Fork testing", false)
    private val scriptForkNetComboBox = ComboBox(arrayOf("", "testnet", "mainnet"))
    private val scriptForkBlockNumberField = JBTextField(null)
    private val scriptApiKeyField = JBTextField(null)
    
    private val scriptBroadcastCheckBox = JBCheckBox("Broadcast", false)
    private val scriptBroadcastNetComboBox = ComboBox(arrayOf("", "testnet", "mainnet"))
    private val scriptExplorerComboBox = ComboBox(arrayOf("", "tonscan", "toncx", "dton", "tonviewer"))

    // Test specific
    private val testTargetBrowseField = TextFieldWithBrowseButton()
    private val testFunctionNameField = JBTextField()
    private val testClearCacheCheckBox = JBCheckBox("Clear compilation cache before testing", false)
    private var testMode: ActonCommand.Test.TestMode = ActonCommand.Test.TestMode.DIRECTORY

    // Run specific
    private val runScriptNameField = createTextFieldWithCompletion(ActonScriptCompletionProvider(project))

    private var buildGroup: Row? = null
    private var scriptGroup: Row? = null
    private var testGroup: Row? = null
    private var runGroup: Row? = null
    private var additionalArgumentsRow: Row? = null

    private var testTargetRow: Row? = null
    private var testFunctionNameRow: Row? = null

    private val knownCommands = listOf(
        "init", "new", "wallet", "test", "wrapper", "script",
        "build", "run", "compile", "disasm", "verify", "retrace",
        "library", "up", "completions"
    )

    init {
        workingDirectoryField.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Select Working Directory")
        )
        buildOutDirField.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Select Output Directory")
        )
        scriptPathField.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor().withTitle("Select Script File")
        )
        testTargetBrowseField.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor().withTitle("Select Test Target")
        )
        
        setupExpandableSupport(commandField)
        setupExpandableSupport(parametersField)
        setupExpandableSupport(buildContractIdField)
        setupExpandableSupport(runScriptNameField)

        commandField.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                updateVisibility()
            }
        })
        
        scriptForkCheckBox.addActionListener { updateEnabledState() }
        scriptBroadcastCheckBox.addActionListener { updateEnabledState() }
    }

    private fun updateVisibility() {
        val command = commandField.text.trim().split(" ").firstOrNull() ?: ""
        val isBuild = command == "build"
        val isScript = command == "script"
        val isTest = command == "test"
        val isRun = command == "run"
        val isKnown = command in knownCommands

        buildGroup?.visible(isBuild)
        scriptGroup?.visible(isScript)
        testGroup?.visible(isTest)
        runGroup?.visible(isRun)
        additionalArgumentsRow?.visible(isKnown)

        if (isTest) {
            testTargetRow?.visible(true)
            testFunctionNameRow?.visible(testMode == ActonCommand.Test.TestMode.FUNCTION)
        }
    }

    private fun updateTestMode(mode: ActonCommand.Test.TestMode) {
        if (testMode == mode) return
        testMode = mode
        if (testMode != ActonCommand.Test.TestMode.FUNCTION) {
            testFunctionNameField.text = ""
        }
        updateVisibility()
    }

    private fun updateEnabledState() {
        val forkEnabled = scriptForkCheckBox.isSelected
        scriptForkNetComboBox.isEnabled = forkEnabled
        scriptForkBlockNumberField.isEnabled = forkEnabled
        scriptApiKeyField.isEnabled = forkEnabled

        val broadcastEnabled = scriptBroadcastCheckBox.isSelected
        scriptBroadcastNetComboBox.isEnabled = broadcastEnabled
        scriptExplorerComboBox.isEnabled = broadcastEnabled
    }

    private fun createTextFieldWithCompletion(provider: TextFieldCompletionProvider?): TextFieldWithCompletion {
        val field = if (provider != null) {
            TextFieldWithCompletion(project, provider, "", true, true, true)
        } else {
            TextFieldWithCompletion(project, object : TextFieldCompletionProvider() {
                override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {}
            }, "", true, true, true)
        }

        field.addSettingsProvider { editor ->
            editor.colorsScheme.editorFontName = JBUI.Fonts.label().fontName
            val settings = (editor as EditorEx).settings
            settings.isWhitespacesShown = false
            settings.isLineNumbersShown = false
            settings.isIndentGuidesShown = false
            settings.isFoldingOutlineShown = false
            editor.setFontSize(JBUI.Fonts.label().size)
            editor.colorsScheme.editorFontName =
                com.intellij.openapi.editor.colors.EditorColorsManager.getInstance().globalScheme.editorFontName
        }
        return field
    }

    private fun setupExpandableSupport(field: EditorTextField) {
        ExpandableEditorSupport(field)
    }

    override fun resetEditorFrom(configuration: ActonCommandConfiguration) {
        commandField.setText(configuration.command)
        parametersField.setText(configuration.parameters)
        workingDirectoryField.text = configuration.workingDirectory?.toString() ?: ""
        environmentVariables.envData = configuration.env
        emulateTerminal.isSelected = configuration.emulateTerminal

        buildContractIdField.setText(configuration.buildContractId)
        buildClearCacheCheckBox.isSelected = configuration.buildClearCache
        buildOutDirField.text = configuration.buildOutDir

        scriptPathField.text = configuration.scriptPath
        scriptClearCacheCheckBox.isSelected = configuration.scriptClearCache
        scriptForkCheckBox.isSelected = configuration.scriptForkNet.isNotEmpty() || configuration.scriptForkBlockNumber.isNotEmpty() || configuration.scriptApiKey.isNotEmpty()
        scriptForkNetComboBox.selectedItem = configuration.scriptForkNet
        scriptForkBlockNumberField.setText(configuration.scriptForkBlockNumber)
        scriptApiKeyField.setText(configuration.scriptApiKey)
        scriptBroadcastCheckBox.isSelected = configuration.scriptBroadcast
        scriptBroadcastNetComboBox.selectedItem = configuration.scriptBroadcastNet
        scriptExplorerComboBox.selectedItem = configuration.scriptExplorer
        
        testMode = configuration.testMode
        testTargetBrowseField.text = configuration.testTarget
        testFunctionNameField.text = configuration.testFunctionName
        testClearCacheCheckBox.isSelected = configuration.testClearCache
        
        runScriptNameField.text = configuration.runScriptName
        
        mainPanel.reset()
        updateVisibility()
        updateEnabledState()
    }

    override fun applyEditorTo(configuration: ActonCommandConfiguration) {
        configuration.command = commandField.text.trim()
        configuration.parameters = parametersField.text.trim()
        configuration.workingDirectory = workingDirectoryField.text.takeIf { it.isNotBlank() }?.let { Paths.get(it) }
        configuration.env = environmentVariables.envData
        configuration.emulateTerminal = emulateTerminal.isSelected

        configuration.buildContractId = buildContractIdField.text.trim()
        configuration.buildClearCache = buildClearCacheCheckBox.isSelected
        configuration.buildOutDir = buildOutDirField.text.trim()

        configuration.scriptPath = scriptPathField.text.trim()
        configuration.scriptClearCache = scriptClearCacheCheckBox.isSelected
        configuration.scriptForkNet = if (scriptForkCheckBox.isSelected) (scriptForkNetComboBox.selectedItem as? String ?: "") else ""
        configuration.scriptForkBlockNumber = if (scriptForkCheckBox.isSelected) scriptForkBlockNumberField.text.trim() else ""
        configuration.scriptApiKey = if (scriptForkCheckBox.isSelected) scriptApiKeyField.text.trim() else ""
        configuration.scriptBroadcast = scriptBroadcastCheckBox.isSelected
        configuration.scriptBroadcastNet = if (scriptBroadcastCheckBox.isSelected) (scriptBroadcastNetComboBox.selectedItem as? String ?: "") else ""
        configuration.scriptExplorer = if (scriptBroadcastCheckBox.isSelected) (scriptExplorerComboBox.selectedItem as? String ?: "") else ""

        configuration.testMode = testMode
        configuration.testTarget = testTargetBrowseField.text.trim()
        configuration.testFunctionName = testFunctionNameField.text.trim()
        configuration.testClearCache = testClearCacheCheckBox.isSelected

        configuration.runScriptName = runScriptNameField.text.trim()
    }

    override fun createEditor(): JComponent {
        mainPanel = panel {
            row("Command:") {
                cell(commandField)
                    .align(AlignX.FILL)
            }.bottomGap(BottomGap.NONE)

            additionalArgumentsRow = row("Additional arguments:") {
                cell(parametersField)
                    .align(AlignX.FILL)
            }.topGap(TopGap.NONE).bottomGap(BottomGap.SMALL)

            buildGroup = group("Build arguments") {
                row("Contract:") {
                    cell(buildContractIdField).align(AlignX.FILL)
                }
                row {
                    cell(buildClearCacheCheckBox)
                }
                row("Output directory:") {
                    cell(buildOutDirField).align(AlignX.FILL)
                }
            }.topGap(TopGap.NONE)

            scriptGroup = group("Script arguments") {
                row("Script path:") {
                    cell(scriptPathField).align(AlignX.FILL)
                }
                row {
                    cell(scriptClearCacheCheckBox)
                }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)

                group("Broadcasting") {
                    row {
                        cell(scriptBroadcastCheckBox).comment("Send transactions to the blockchain instead of emulating them")
                    }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)
                    row("Network:") {
                        cell(scriptBroadcastNetComboBox).align(AlignX.FILL)
                    }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)
                    row("Explorer:") {
                        cell(scriptExplorerComboBox).align(AlignX.FILL).comment("Explorer to use for transaction links")
                    }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)
                }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)
            }.topGap(TopGap.NONE)

            testGroup = group("Test arguments") {
                buttonsGroup {
                    row("Test mode:") {
                        radioButton("Directory", ActonCommand.Test.TestMode.DIRECTORY)
                            .onChanged { if (it.isSelected) updateTestMode(ActonCommand.Test.TestMode.DIRECTORY) }
                        radioButton("File", ActonCommand.Test.TestMode.FILE)
                            .onChanged { if (it.isSelected) updateTestMode(ActonCommand.Test.TestMode.FILE) }
                        radioButton("Function", ActonCommand.Test.TestMode.FUNCTION)
                            .onChanged { if (it.isSelected) updateTestMode(ActonCommand.Test.TestMode.FUNCTION) }
                    }
                }.bind({ testMode }, { testMode = it })

                testTargetRow = row("Target path:") {
                    cell(testTargetBrowseField).align(AlignX.FILL)
                }

                testFunctionNameRow = row("Name:") {
                    cell(testFunctionNameField).align(AlignX.FILL)
                }

                row {
                    cell(testClearCacheCheckBox)
                }
            }.topGap(TopGap.NONE)

            runGroup = group("Run arguments") {
                row("Script name:") {
                    cell(runScriptNameField).align(AlignX.FILL)
                }
            }.topGap(TopGap.NONE)

            row {
                cell(emulateTerminal)
            }
            row(environmentVariables.label) {
                cell(environmentVariables)
                    .align(AlignX.FILL)
            }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)
            row("Working directory:") {
                cell(workingDirectoryField)
                    .align(AlignX.FILL)
            }.topGap(TopGap.NONE).bottomGap(BottomGap.NONE)
        }
        mainPanel.apply {
            updateVisibility()
            updateEnabledState()
        }
        return mainPanel
    }
}
