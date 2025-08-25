package org.ton.intellij.tolk.refactor

import com.intellij.openapi.options.ConfigurationException
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.ui.NameSuggestionsField
import com.intellij.refactoring.ui.RefactoringDialog
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.ton.intellij.tolk.TolkFileType
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class TolkIntroduceVariableDialog(private val operation: TolkIntroduceOperation) : RefactoringDialog(operation.project, true) {
    private val occurrencesCount = operation.occurrences.size
    private var nameField: NameSuggestionsField? = null
    private var replaceAllCheckBox: JCheckBox? = null

    init {
        title = RefactoringBundle.message("introduce.variable.title")
        isModal = true

        init()
    }

    override fun hasHelpAction() = false

    override fun hasPreviewButton() = false

    override fun getPreferredFocusedComponent() = nameField

    override fun canRun() {
        if (!areButtonsValid()) {
            throw ConfigurationException(RefactoringBundle.message("refactoring.introduce.name.error"), name)
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.add(createNamePanel(), BorderLayout.CENTER)
        if (occurrencesCount > 1) {
            panel.add(createReplaceAllPanel(), BorderLayout.SOUTH)
        }
        panel.preferredSize = Dimension(nameField!!.width, -1)
        return panel
    }

    private fun createNamePanel(): JComponent {
        val panel = JPanel(BorderLayout())
        nameField = NameSuggestionsField(emptyArray(), operation.project, TolkFileType)
        nameField!!.border = JBUI.Borders.empty(3, 5, 2, 3)
        nameField!!.addDataChangedListener { validateButtons() }
        val label = JLabel(UIUtil.replaceMnemonicAmpersand(RefactoringBundle.message("name.prompt")))
        label.labelFor = nameField
        panel.add(nameField!!, BorderLayout.CENTER)
        panel.add(label, BorderLayout.WEST)
        return panel
    }

    private fun createReplaceAllPanel(): JComponent {
        val panel = JPanel(FlowLayout())
        val text = UIUtil.replaceMnemonicAmpersand(RefactoringBundle.message("replace.all.occurences", occurrencesCount))
        replaceAllCheckBox = JCheckBox(text)
        panel.add(replaceAllCheckBox)
        return panel
    }

    override fun areButtonsValid() = true

    override fun doAction() {
        closeOKAction()
    }

    val name: String get() = nameField!!.enteredName
    val replaceAll: Boolean get() = replaceAllCheckBox != null && replaceAllCheckBox!!.isSelected
}
