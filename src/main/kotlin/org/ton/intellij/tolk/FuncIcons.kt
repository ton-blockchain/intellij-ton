package org.ton.intellij.tolk

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader

object TolkIcons {
    val FILE = IconLoader.getIcon("/icons/ton_gradient.svg", TolkIcons::class.java)
    val FUNCTION = AllIcons.Nodes.Function
    val PARAMETER = AllIcons.Nodes.Parameter
    val CONSTANT = AllIcons.Nodes.Constant
    val VARIABLE = AllIcons.Nodes.Variable
    val GLOBAL_VARIABLE = AllIcons.Nodes.Gvariable
    val RECURSIVE_CALL = AllIcons.Gutter.RecursiveMethod
}
