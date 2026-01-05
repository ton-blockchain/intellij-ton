package org.ton.intellij.acton

object ActonUtils {
    fun stripAnsiColors(text: String): String {
        return text.replace("\u001B\\[[;?0-9]*[a-zA-Z]".toRegex(), "")
    }
}
