package org.ton.intellij.tolk.ide.configurable

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.util.pathToDirectoryTextField

class TolkProjectConfigurable(
    val project: Project,
) : BoundConfigurable("Tolk"), Configurable.NoScroll, Disposable {

    data class Data(
        val stdlibPath: String?,
    )

    private val pathToStdlibField = pathToDirectoryTextField(this, TolkBundle["settings.tolk.toolchain.stdlib.dialog.title"])

    var data: Data
        get() {
            val stdlibPath = pathToStdlibField.text.ifBlank { null }
            return Data(
                stdlibPath = stdlibPath
            )
        }
        set(value) {
            pathToStdlibField.text = value.stdlibPath ?: ""
        }

    override fun createPanel(): DialogPanel = panel {
        row(TolkBundle["settings.tolk.toolchain.stdlib.label"]) {
            cell(pathToStdlibField).align(AlignX.FILL)
        }

        val setting = project.tolkSettings
        onApply {
            setting.stdlibPath = data.stdlibPath
        }
        onReset {
            val currentData = data
            val newData = Data(
                stdlibPath = setting.stdlibPath
            )
            if (currentData != newData) {
                data = newData
            }
        }
        onIsModified {
            val data = data
            data.stdlibPath != setting.stdlibPath
        }
    }

    override fun dispose() {
        Disposer.dispose(this)
    }
}
