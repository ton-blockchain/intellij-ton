package org.ton.intellij.tolk

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader

object TolkIcons {
    val FILE = IconLoader.getIcon("/icons/tolk_file.svg", TolkIcons::class.java)
    val FUNCTION = AllIcons.Nodes.Function
    val METHOD = AllIcons.Nodes.Method
    val PARAMETER = AllIcons.Nodes.Parameter
    val CONSTANT = AllIcons.Nodes.Constant
    val VARIABLE = AllIcons.Nodes.Variable
    val ANNOTATION = AllIcons.Nodes.Annotationtype
    val FIELD = IconLoader.getIcon("/icons/field.svg", TolkIcons::class.java)
    val STRUCTURE = IconLoader.getIcon("/icons/struct.svg", TolkIcons::class.java)
    val TYPE_ALIAS = IconLoader.getIcon("/icons/typeAlias.svg", TolkIcons::class.java)
    val GLOBAL_VARIABLE = IconLoader.getIcon("/icons/globalVariable.svg", TolkIcons::class.java)
}
