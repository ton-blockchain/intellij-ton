package org.ton.intellij.acton.settings

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class ActonExplorer(val id: String, private val displayName: String) {
    TONVIEWER("tonviewer", "Tonviewer"),
    TONSCAN("tonscan", "Tonscan"),
    ;

    override fun toString(): String = displayName

    fun addressUrl(address: String, isTestnet: Boolean): String {
        val encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        return when (this) {
            TONVIEWER -> {
                val domain = if (isTestnet) "testnet.tonviewer.com" else "tonviewer.com"
                "https://$domain/$encodedAddress"
            }

            TONSCAN -> {
                val domain = if (isTestnet) "testnet.tonscan.org" else "tonscan.org"
                "https://$domain/address/$encodedAddress"
            }
        }
    }

    companion object {
        val DEFAULT = TONVIEWER

        fun fromId(id: String?): ActonExplorer = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
