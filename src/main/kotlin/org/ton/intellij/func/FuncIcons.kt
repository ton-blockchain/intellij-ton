package org.ton.intellij.func

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader

object FuncIcons {
    val FILE = IconLoader.getIcon("/icons/fc.svg", FuncIcons::class.java)
    val FUNCTION = AllIcons.Nodes.Function
    val CONSTANT = AllIcons.Nodes.Constant
    val PARAMETER = AllIcons.Nodes.Parameter
    val VARIABLE = AllIcons.Nodes.Variable
}