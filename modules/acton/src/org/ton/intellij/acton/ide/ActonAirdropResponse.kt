package org.ton.intellij.acton.ide

import com.google.gson.Gson
import org.ton.intellij.acton.ActonUtils.stripAnsiColors

internal data class ActonAirdropResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val error: String? = null,
)

private val ACTON_AIRDROP_RESPONSE_GSON = Gson()
private val FAUCET_ERROR_STATUS_PATTERN =
    Regex("""^(Faucet returned error \d{3})(?: [^:]+)?:\s*(.*)$""")

internal fun parseActonAirdropResponse(output: String): ActonAirdropResponse? {
    val cleaned = stripAnsiColors(output).trim()
    if (cleaned.isEmpty()) return null

    val candidates = buildList {
        add(cleaned)
        cleaned.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach(::add)

        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start >= 0 && end > start) {
            add(cleaned.substring(start, end + 1))
        }
    }.distinct()

    return candidates.asReversed().firstNotNullOfOrNull { candidate ->
        runCatching {
            ACTON_AIRDROP_RESPONSE_GSON.fromJson(candidate, ActonAirdropResponse::class.java)
        }.getOrNull()?.takeIf { response ->
            response.success != null || response.message != null || response.error != null
        }
    }
}

internal fun renderActonAirdropError(
    response: ActonAirdropResponse?,
    stdout: String,
    stderr: String,
    exitCode: Int,
): String {
    response?.error
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { return renderEmbeddedFaucetError(it) }

    if (response?.success != true) {
        response?.message
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { return renderEmbeddedFaucetError(it) }
    }

    val output = sequenceOf(stderr, stdout)
        .map { stripAnsiColors(it).trim() }
        .firstOrNull(String::isNotEmpty)
        ?.let(::removeErrorPrefix)

    return output ?: "Faucet request failed (exit code $exitCode)"
}

private fun renderEmbeddedFaucetError(error: String): String {
    val cleaned = stripAnsiColors(error).trim()
    val nestedResponse = parseActonAirdropResponse(cleaned)
    val nestedMessage = (nestedResponse?.error ?: nestedResponse?.message)
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: return normalizeFaucetErrorStatus(cleaned)

    val jsonStart = cleaned.indexOf('{')
    if (jsonStart < 0) return normalizeFaucetErrorStatus(nestedMessage)

    val prefix = cleaned.substring(0, jsonStart).trim().trimEnd(':').trim()
    val rendered = if (prefix.isEmpty()) nestedMessage else "$prefix: $nestedMessage"
    return normalizeFaucetErrorStatus(rendered)
}

private fun normalizeFaucetErrorStatus(error: String): String {
    val match = FAUCET_ERROR_STATUS_PATTERN.matchEntire(error) ?: return error
    val prefix = match.groupValues[1]
    val message = match.groupValues[2].trim()
    return if (message.isEmpty()) prefix else "$prefix: $message"
}

private fun removeErrorPrefix(output: String): String {
    val firstLine = output.lineSequence().firstOrNull()?.trimStart() ?: return output
    if (!firstLine.startsWith("Error:")) return output
    return output.replaceFirst("Error:", "").trimStart()
}
