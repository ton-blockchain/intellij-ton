package org.ton.intellij.acton.ide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActonAirdropResponseTest {
    @Test
    fun `parses successful airdrop response`() {
        val response = parseActonAirdropResponse(
            """
                {"success":true,"message":"Your claim has been queued.","address":"0:test"}
            """.trimIndent(),
        )

        assertEquals(true, response?.success)
        assertEquals("Your claim has been queued.", response?.message)
        assertNull(response?.error)
    }

    @Test
    fun `parses faucet error response surrounded by process output`() {
        val response = parseActonAirdropResponse(
            """
                Requesting faucet claim
                {"success":false,"error":"Faucet returned error 429: Too many requests"}
                Error: Faucet returned error 429: Too many requests
            """.trimIndent(),
        )

        assertEquals(false, response?.success)
        assertEquals("Faucet returned error 429: Too many requests", response?.error)
    }

    @Test
    fun `parses raw faucet error response`() {
        val response = parseActonAirdropResponse(
            """
                {"error":"Invalid or expired challenge"}
            """.trimIndent(),
        )

        assertNull(response?.success)
        assertEquals("Invalid or expired challenge", response?.error)
    }

    @Test
    fun `renders structured faucet error before stderr`() {
        val response = ActonAirdropResponse(
            success = false,
            error = "Faucet returned error 503: PoW is disabled",
        )

        assertEquals(
            "Faucet returned error 503: PoW is disabled",
            renderActonAirdropError(response, "", "Error: another message", 1),
        )
    }

    @Test
    fun `renders nested faucet error body without raw json`() {
        val response = ActonAirdropResponse(
            success = false,
            error = "Failed to get challenge: status 403 Forbidden: {\"error\":\"Wallet balance exceeds limit\"}",
        )

        assertEquals(
            "Failed to get challenge: status 403 Forbidden: Wallet balance exceeds limit",
            renderActonAirdropError(response, "", "", 1),
        )
    }

    @Test
    fun `renders faucet status without repeating the http reason`() {
        val response = ActonAirdropResponse(
            success = false,
            error = "Faucet returned error 429 Too Many Requests: Too many requests from your IP",
        )

        assertEquals(
            "Faucet returned error 429: Too many requests from your IP",
            renderActonAirdropError(response, "", "", 1),
        )
    }

    @Test
    fun `renders cli error without the generic Error prefix`() {
        assertEquals(
            "Faucet returned error 500: service unavailable",
            renderActonAirdropError(
                response = null,
                stdout = "",
                stderr = "\u001B[31mError:\u001B[0m Faucet returned error 500: service unavailable\n",
                exitCode = 1,
            ),
        )
    }

    @Test
    fun `renders exit code when faucet returns no details`() {
        assertEquals(
            "Faucet request failed (exit code 1)",
            renderActonAirdropError(null, "", "", 1),
        )
    }
}
