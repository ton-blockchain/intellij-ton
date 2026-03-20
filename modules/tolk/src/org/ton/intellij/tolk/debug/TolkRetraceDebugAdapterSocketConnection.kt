package org.ton.intellij.tolk.debug

import com.intellij.execution.ExecutionException
import com.intellij.openapi.diagnostic.logger
import com.intellij.platform.dap.connection.DebugAdapterHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

private val LOG = logger<TolkRetraceDebugAdapterSocketHandle>()
private val SEQ_REGEX = """"seq"\s*:\s*(\d+)""".toRegex()

internal suspend fun connectTolkRetraceDebugAdapterSocket(
    host: String,
    port: Int,
    onDisconnect: suspend () -> Unit
): DebugAdapterHandle {
    var lastError: Throwable? = null
    repeat(3) { attempt ->
        try {
            LOG.info("Connecting to retrace DAP socket $host:$port (attempt ${attempt + 1}/3)")
            return TolkRetraceDebugAdapterSocketHandle(host, port, onDisconnect)
        } catch (t: Throwable) {
            lastError = t
            LOG.warn("Failed to connect to retrace DAP socket $host:$port on attempt ${attempt + 1}/3", t)
            delay(300)
        }
    }
    throw ExecutionException(lastError ?: IllegalStateException("Failed to connect to retrace DAP socket"))
}

internal fun normalizeDapMessages(
    input: InputStream,
    output: OutputStream,
    onMessage: ((List<Pair<String, String>>, ByteArray) -> Unit)? = null
) {
    while (true) {
        val message = readDapMessage(input) ?: return
        val headers = message.headers
        val body = message.body
        onMessage?.invoke(headers, body)
        writeDapMessage(output, headers, body)
    }
}

private data class DapMessage(
    val headers: List<Pair<String, String>>,
    val body: ByteArray
)

private fun readDapMessage(input: InputStream): DapMessage? {
    val headers = readDapHeaders(input) ?: return null
    val contentLength = headers.firstNotNullOfOrNull { (name, value) ->
        if (name.equals("Content-Length", ignoreCase = true)) value.toIntOrNull() else null
    } ?: throw IOException("Missing Content-Length header")

    val body = ByteArray(contentLength)
    var offset = 0
    while (offset < contentLength) {
        val read = input.read(body, offset, contentLength - offset)
        if (read == -1) {
            throw EOFException("Unexpected EOF while reading DAP body")
        }
        offset += read
    }

    return DapMessage(headers, body)
}

private fun writeDapMessage(
    output: OutputStream,
    headers: List<Pair<String, String>>,
    body: ByteArray
) {
    for ((name, value) in headers) {
        output.write("$name: $value\r\n".toByteArray(StandardCharsets.US_ASCII))
    }
    output.write("\r\n".toByteArray(StandardCharsets.US_ASCII))
    output.write(body)
    output.flush()
}

private fun readDapHeaders(input: InputStream): List<Pair<String, String>>? {
    val headers = mutableListOf<Pair<String, String>>()
    while (true) {
        val line = readAsciiLine(input) ?: return if (headers.isEmpty()) null else throw EOFException("Unexpected EOF in DAP headers")
        if (line.isEmpty()) {
            if (headers.isEmpty()) {
                continue
            }
            return headers
        }

        val separatorIndex = line.indexOf(':')
        if (separatorIndex <= 0) {
            throw IOException("Invalid DAP header: $line")
        }

        headers += line.substring(0, separatorIndex).trim() to line.substring(separatorIndex + 1).trim()
    }
}

private fun readAsciiLine(input: InputStream): String? {
    val buffer = ByteArrayOutputStream()
    while (true) {
        val next = input.read()
        if (next == -1) {
            return if (buffer.size() == 0) null else buffer.toString(StandardCharsets.US_ASCII)
        }
        when (next.toChar()) {
            '\n' -> return buffer.toString(StandardCharsets.US_ASCII)
            '\r' -> Unit
            else -> buffer.write(next)
        }
    }
}

private class TolkRetraceDebugAdapterSocketHandle(
    host: String,
    port: Int,
    private val onDisconnect: suspend () -> Unit
) : DebugAdapterHandle {
    private val socket = try {
        Socket(host, port)
    } catch (e: IOException) {
        throw ExecutionException(e)
    }
    private val socketInput = socket.getInputStream()
    override val output: OutputStream = LoggingDapOutputStream(
        socket.getOutputStream(),
        "IDE -> retrace ($host:$port)"
    )
    private val normalizedInput = PipedInputStream(64 * 1024)
    override val input: InputStream = normalizedInput
    private val normalizedInputWriter = PipedOutputStream(normalizedInput)
    private val disconnected = AtomicBoolean(false)
    private val configDoneAcknowledged = AtomicBoolean(false)
    private val sawStopLikeEvent = AtomicBoolean(false)
    private val syntheticInitialStopInjected = AtomicBoolean(false)
    private val lastIncomingSeq = AtomicLong(0)
    private val syntheticSeq = AtomicLong(1_000_000)
    private val normalizedWriteLock = Any()
    private val pumpThread = thread(
        name = "Tolk retrace DAP normalizer $port",
        start = false,
        isDaemon = true
    ) {
        pumpInput()
    }

    init {
        LOG.info("Connected to retrace DAP socket ${socket.remoteSocketAddress} from ${socket.localSocketAddress}")
        pumpThread.start()
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            if (!disconnected.compareAndSet(false, true)) {
                return@withContext
            }
            LOG.info("Disconnecting retrace DAP socket ${socket.remoteSocketAddress}")
            runCatching { socket.close() }
            runCatching { normalizedInputWriter.close() }
            runCatching { input.close() }
            onDisconnect()
        }
    }

    private fun pumpInput() {
        try {
            while (true) {
                val message = readDapMessage(socketInput) ?: break
                logDapMessage("retrace -> IDE (${socket.remoteSocketAddress})", message.headers, message.body)
                observeIncomingMessage(message.body)
                writeNormalizedMessage(message.headers, message.body)
            }
        } catch (e: Throwable) {
            if (!disconnected.get()) {
                LOG.warn("Failed to normalize retrace DAP input", e)
            }
        } finally {
            runCatching { normalizedInputWriter.close() }
        }
    }

    private fun observeIncomingMessage(body: ByteArray) {
        val bodyText = String(body, StandardCharsets.UTF_8)
        SEQ_REGEX.find(bodyText)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let(lastIncomingSeq::set)

        if (bodyText.contains("\"type\":\"event\"")) {
            when {
                bodyText.contains("\"event\":\"stopped\"") -> {
                    sawStopLikeEvent.set(true)
                    LOG.info("Retrace DAP reported a native stopped event")
                }
                bodyText.contains("\"event\":\"terminated\"") || bodyText.contains("\"event\":\"exited\"") -> {
                    sawStopLikeEvent.set(true)
                    LOG.info("Retrace DAP reported termination before an initial stop")
                }
            }
        }

        if (bodyText.contains("\"type\":\"response\"") && bodyText.contains("\"command\":\"configurationDone\"")) {
            configDoneAcknowledged.set(true)
            LOG.info("Retrace DAP acknowledged configurationDone")
            return
        }

        if (
            configDoneAcknowledged.get() &&
            !sawStopLikeEvent.get() &&
            !syntheticInitialStopInjected.get() &&
            bodyText.contains("\"type\":\"response\"") &&
            bodyText.contains("\"command\":\"threads\"")
        ) {
            injectSyntheticInitialStop()
        }
    }

    private fun injectSyntheticInitialStop() {
        if (!syntheticInitialStopInjected.compareAndSet(false, true)) {
            return
        }
        if (disconnected.get() || sawStopLikeEvent.get()) {
            return
        }

        val seq = maxOf(syntheticSeq.incrementAndGet(), lastIncomingSeq.get() + 1)
        val bodyText = """
            {"type":"event","seq":$seq,"event":"stopped","body":{"reason":"entry","threadId":1,"allThreadsStopped":true}}
        """.trimIndent()
        val body = bodyText.toByteArray(StandardCharsets.UTF_8)
        LOG.info("Retrace DAP produced no initial stopped event; injecting synthetic stopped(entry) with seq=$seq")
        writeNormalizedMessage(listOf("Content-Length" to body.size.toString()), body)
    }

    private fun writeNormalizedMessage(headers: List<Pair<String, String>>, body: ByteArray) {
        synchronized(normalizedWriteLock) {
            writeDapMessage(normalizedInputWriter, headers, body)
        }
    }
}

private class LoggingDapOutputStream(
    private val delegate: OutputStream,
    private val directionLabel: String
) : OutputStream() {
    private val buffer = ByteArrayOutputStream()

    override fun write(b: Int) {
        delegate.write(b)
        buffer.write(b)
        drainMessages()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        delegate.write(b, off, len)
        buffer.write(b, off, len)
        drainMessages()
    }

    override fun flush() {
        delegate.flush()
    }

    override fun close() {
        delegate.close()
    }

    private fun drainMessages() {
        val bytes = buffer.toByteArray()
        var offset = 0
        while (true) {
            val message = parseBufferedDapMessage(bytes, offset) ?: break
            logDapMessage(directionLabel, message.headers, message.body)
            offset = message.nextOffset
        }
        if (offset == 0) {
            return
        }
        buffer.reset()
        buffer.write(bytes, offset, bytes.size - offset)
    }
}

private data class BufferedDapMessage(
    val headers: List<Pair<String, String>>,
    val body: ByteArray,
    val nextOffset: Int
)

private fun parseBufferedDapMessage(bytes: ByteArray, startOffset: Int): BufferedDapMessage? {
    val headerEnd = findHeaderEnd(bytes, startOffset) ?: return null
    val headerBytes = bytes.copyOfRange(startOffset, headerEnd)
    val headerText = String(headerBytes, StandardCharsets.US_ASCII)
    val headers = headerText
        .split("\r\n")
        .filter { it.isNotBlank() }
        .map { line ->
            val separatorIndex = line.indexOf(':')
            if (separatorIndex <= 0) {
                throw IOException("Invalid DAP header in buffered output: $line")
            }
            line.substring(0, separatorIndex).trim() to line.substring(separatorIndex + 1).trim()
        }
    val contentLength = headers.firstNotNullOfOrNull { (name, value) ->
        if (name.equals("Content-Length", ignoreCase = true)) value.toIntOrNull() else null
    } ?: throw IOException("Missing Content-Length header in buffered output")
    val bodyStart = headerEnd + 4
    val bodyEnd = bodyStart + contentLength
    if (bytes.size < bodyEnd) {
        return null
    }
    return BufferedDapMessage(headers, bytes.copyOfRange(bodyStart, bodyEnd), bodyEnd)
}

private fun findHeaderEnd(bytes: ByteArray, startOffset: Int): Int? {
    for (index in startOffset..bytes.size - 4) {
        if (bytes[index] == '\r'.code.toByte() &&
            bytes[index + 1] == '\n'.code.toByte() &&
            bytes[index + 2] == '\r'.code.toByte() &&
            bytes[index + 3] == '\n'.code.toByte()
        ) {
            return index
        }
    }
    return null
}

private fun logDapMessage(directionLabel: String, headers: List<Pair<String, String>>, body: ByteArray) {
    val headersText = headers.joinToString(", ") { "${it.first}: ${it.second}" }
    val bodyText = String(body, StandardCharsets.UTF_8)
        .replace("\r", "\\r")
        .replace("\n", "\\n")
    LOG.info("$directionLabel headers=[$headersText] body=$bodyText")
}
