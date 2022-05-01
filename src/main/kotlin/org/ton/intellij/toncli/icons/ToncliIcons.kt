package org.ton.intellij.toncli.icons

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object ToncliIcons {
    val ICON = load("/icons/disintar.png")
    val LOCK_ICON = ICON
    val TEST = AllIcons.RunConfigurations.TestState.Run
    val TEST_GREEN = AllIcons.RunConfigurations.TestState.Green2
    val TEST_RED = AllIcons.RunConfigurations.TestState.Red2

    private fun load(path: String): Icon = IconLoader.getIcon(path, ToncliIcons::class.java)
}