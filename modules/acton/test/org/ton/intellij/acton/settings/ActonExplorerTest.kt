package org.ton.intellij.acton.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ActonExplorerTest {
    @Test
    fun `actonscan is first and default explorer`() {
        assertEquals(ActonExplorer.ACTONSCAN, ActonExplorer.entries.first())
        assertEquals(ActonExplorer.ACTONSCAN, ActonExplorer.DEFAULT)
        assertEquals(ActonExplorer.ACTONSCAN, ActonExplorer.fromId(null))
    }

    @Test
    fun `actonscan links include the mainnet network`() {
        assertEquals(
            "https://actonscan.com/address/EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c?network=mainnet",
            ActonExplorer.ACTONSCAN.addressUrl(
                "EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c",
                isTestnet = false,
            ),
        )
    }

    @Test
    fun `actonscan links include the testnet network`() {
        assertEquals(
            "https://actonscan.com/address/kQB6XGzpO7rglhK1tR9A4l2QQu6yaYE6ALUp1vAOHMaGAfGD?network=testnet",
            ActonExplorer.ACTONSCAN.addressUrl(
                "kQB6XGzpO7rglhK1tR9A4l2QQu6yaYE6ALUp1vAOHMaGAfGD",
                isTestnet = true,
            ),
        )
    }
}
