package org.ton.intellij.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.ui.DocumentAdapter
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

fun pathToDirectoryTextField(
    disposable: Disposable,
    @DialogTitle title: String,
    onTextChanged: () -> Unit = {}
): TextFieldWithBrowseButton =
    pathTextField(
        FileChooserDescriptorFactory.createSingleFolderDescriptor(),
        disposable,
        title,
        onTextChanged
    )

fun pathToExecutableTextField(
    disposable: Disposable,
    @DialogTitle title: String,
    onTextChanged: () -> Unit = {}
): TextFieldWithBrowseButton =
    pathTextField(
        FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
        disposable,
        title,
        onTextChanged
    )

fun pathTextField(
    fileChooserDescriptor: FileChooserDescriptor,
    disposable: Disposable,
    @DialogTitle title: String,
    onTextChanged: () -> Unit = {}
): TextFieldWithBrowseButton {
    val component = TextFieldWithBrowseButton(null, disposable)
    component.addBrowseFolderListener(
        title, null, null,
        fileChooserDescriptor,
        TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
    )
    component.childComponent.addTextChangeListener { onTextChanged() }
    return component
}

fun JTextField.addTextChangeListener(listener: (DocumentEvent) -> Unit) {
    document.addDocumentListener(
        object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                listener(e)
            }
        }
    )
}
