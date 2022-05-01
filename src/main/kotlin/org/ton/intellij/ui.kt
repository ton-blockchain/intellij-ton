package org.ton.intellij

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.util.Alarm
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

class UiDebouncer(
        private val parentDisposable: Disposable,
        private val delayMillis: Int = 200
) {
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable)

    /**
     * @param onUiThread: callback to be executed in EDT with **any** modality state.
     * Use it only for UI updates
     */
    fun <T> run(onPooledThread: () -> T, onUiThread: (T) -> Unit) {
        if (Disposer.isDisposed(parentDisposable)) return
        alarm.cancelAllRequests()
        alarm.addRequest({
            val r = onPooledThread()
            invokeLater(ModalityState.any()) {
                if (!Disposer.isDisposed(parentDisposable)) {
                    onUiThread(r)
                }
            }
        }, delayMillis)
    }
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