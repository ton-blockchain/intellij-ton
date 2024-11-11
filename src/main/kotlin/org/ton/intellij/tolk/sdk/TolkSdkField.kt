package org.ton.intellij.tolk.sdk

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import java.awt.Component
import javax.swing.JList
import javax.swing.ListCellRenderer

class TolkSdkField(
    private val project: Project
) : ComponentWithBrowseButton<ComboBox<TolkSdkRef>>(
    KeyEventAwareComboBox(),
    null
) {
    init {
        val childComponent = childComponent
        childComponent.selectedIndex = 0
    }

    private class KeyEventAwareComboBox : ComboBox<TolkSdkRef>() {

    }
}

class TolkSdkRenderer(
    private val project: Project,
) : ListCellRenderer<TolkSdkRef> {
    override fun getListCellRendererComponent(
        list: JList<out TolkSdkRef>?,
        value: TolkSdkRef?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        TODO("Not yet implemented")
    }
}
