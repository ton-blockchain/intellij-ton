package org.ton.intellij.tolk

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader

object TolkIcons {
    val FILE = IconLoader.getIcon("/icons/tolk_file.svg", TolkIcons::class.java)
    val FUNCTION = AllIcons.Nodes.Function
    val PARAMETER = AllIcons.Nodes.Parameter
    val CONSTANT = AllIcons.Nodes.Constant
    val VARIABLE = AllIcons.Nodes.Variable
    val GLOBAL_VARIABLE = IconLoader.getIcon("/icons/globalVariable.svg", TolkIcons::class.java)
    val RECURSIVE_CALL = AllIcons.Gutter.RecursiveMethod
}
