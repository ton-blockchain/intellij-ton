package org.ton.intellij.tolk.debug

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class TolkRetraceDebugAdapterSocketConnectionTest {
    @Test
    fun `test normalizes blank lines between dap messages`() {
        val firstBody = """{"seq":1,"type":"event","event":"initialized"}"""
        val secondBody = """{"seq":2,"type":"response","request_seq":1,"success":true,"command":"launch"}"""
        val input = buggyMessage(firstBody) + "\r\n" + buggyMessage(secondBody)
        val output = ByteArrayOutputStream()

        normalizeDapMessages(
            ByteArrayInputStream(input.toByteArray(StandardCharsets.UTF_8)),
            output
        )

        assertEquals(
            cleanMessage(firstBody) + cleanMessage(secondBody),
            String(output.toByteArray(), StandardCharsets.UTF_8)
        )
    }

    private fun buggyMessage(body: String): String {
        val contentLength = body.toByteArray(StandardCharsets.UTF_8).size
        return "Content-Length: $contentLength\r\n\r\n$body\r\n"
    }

    private fun cleanMessage(body: String): String {
        val contentLength = body.toByteArray(StandardCharsets.UTF_8).size
        return "Content-Length: $contentLength\r\n\r\n$body"
    }
}
