package com.github.andreypfau.intellijton

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

object Plugin {
    val pluginId: PluginId? by lazy { PluginId.getId("com.github.andreypfau.intellij-ton") }
    private val plugin: IdeaPluginDescriptor? by lazy { PluginManagerCore.getPlugin(pluginId) }

    val version: Version? by lazy { plugin?.version?.let { Version(it) } }

    class Version(private val asString: String) {
        override fun toString(): String = asString
        val isStable = asString.matches(Regex("""\d+\.\d+\.\d+"""))
    }
}
