package org.ton.intellij.tact

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader

object TactIcons {
    val FILE = IconLoader.getIcon("/icons/tact_dark.svg", TactIcons::class.java)
    val FUNCTION = AllIcons.Nodes.Function
    val PARAMETER = AllIcons.Nodes.Parameter
    val CONSTANT = AllIcons.Nodes.Constant
    val VARIABLE = AllIcons.Nodes.Variable
    val GLOBAL_VARIABLE = AllIcons.Nodes.Gvariable
    val RECURSIVE_CALL = AllIcons.Gutter.RecursiveMethod
}
