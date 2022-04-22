package org.ton.intellij

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

object Plugin {
    val pluginId: PluginId? by lazy { PluginId.getId("com.github.andreypfau.intellij-ton") }
    private val plugin: IdeaPluginDescriptor? by lazy { PluginManagerCore.getPlugin(org.ton.intellij.Plugin.pluginId) }

    val version: org.ton.intellij.Plugin.Version? by lazy {
        org.ton.intellij.Plugin.plugin?.version?.let {
            org.ton.intellij.Plugin.Version(
                it
            )
        }
    }

    class Version(private val asString: String) {
        override fun toString(): String = asString
        val isStable = asString.matches(Regex("""\d+\.\d+\.\d+"""))
    }
}
