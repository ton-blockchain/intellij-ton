package org.ton.intellij.tolk.ide.imports

import com.intellij.util.NotNullFunction
import com.intellij.util.ui.UIUtil
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

class SelectionAwareListCellRenderer<T>(private val myFun: NotNullFunction<in T, out JComponent>) : ListCellRenderer<T> {
    override fun getListCellRendererComponent(
        list: JList<out T>,
        value: T,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        @Suppress("UNCHECKED_CAST")
        val comp: JComponent = myFun.`fun`(value as T?)
        comp.setOpaque(true)
        if (isSelected) {
            comp.setBackground(list.selectionBackground)
            comp.setForeground(list.selectionForeground)
        } else {
            comp.setBackground(list.getBackground())
            comp.setForeground(list.getForeground())
        }
        for (label in UIUtil.findComponentsOfType(comp, JLabel::class.java)) {
            label.setForeground(UIUtil.getListForeground(isSelected, true))
        }
        return comp
    }
}
