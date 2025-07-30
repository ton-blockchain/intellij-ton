package org.ton.intellij.util.asm

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import com.intellij.psi.PsiElement
import kotlinx.serialization.json.JsonElement
import java.io.InputStream

@Serializable
data class FiftExample(
    val fift: String,
    val description: String,
)

@Serializable
data class AsmDoc(
    val opcode: String,
    val stack: String,
    val category: String,
    val description: String,
    val gas: String,
    val fift: String,
    @SerialName("fift_examples")
    val fiftExamples: List<FiftExample>,
)

@Serializable
data class AsmAlias(
    val mnemonic: String,
    @SerialName("alias_of")
    val aliasOf: String,
    @SerialName("doc_fift")
    val docFift: String? = null,
    @SerialName("doc_stack")
    val docStack: String? = null,
    val description: String? = null,
    val operands: Map<String, JsonElement>,
)

@Serializable
data class AsmInstruction(
    val mnemonic: String,
    val doc: AsmDoc,
    @SerialName("since_version")
    val sinceVersion: Int,
    @SerialName("alias_info")
    val aliasInfo: AsmAlias? = null,
)

@Serializable
data class AsmData(
    val instructions: List<AsmInstruction>,
    val aliases: List<AsmAlias>,
)

object AsmDataProvider {
    private const val RESOURCE_PATH = "asm/asm.json"
    private val json = Json { ignoreUnknownKeys = true }
    private var data: AsmData? = null

    fun getAsmData(): AsmData = data ?: synchronized(this) {
        data ?: loadData().also { data = it }
    }

    private fun loadData(): AsmData {
        val stream: InputStream = javaClass.classLoader.getResourceAsStream(RESOURCE_PATH)
            ?: error("Resource '$RESOURCE_PATH' not found")
        return stream.use { json.decodeFromString(AsmData.serializer(), it.readBytes().toString(Charsets.UTF_8)) }
    }
}

fun findInstruction(name: String, args: List<PsiElement>): AsmInstruction? {
    val asmData = AsmDataProvider.getAsmData()
    val realName = adjustName(name, args)
    // direct lookup by mnemonic
    val direct = asmData.instructions.find { it.mnemonic == realName }
    if (direct != null) return direct
    // alias lookup
    val alias = asmData.aliases.find { it.mnemonic == name }
    if (alias != null) {
        val target = asmData.instructions.find { it.mnemonic == alias.aliasOf }
        if (target != null) {
            return target.copy(aliasInfo = alias)
        }
    }
    return null
}

fun adjustName(name: String, args: List<PsiElement>): String {
    return when (name) {
        "PUSHINT", "INT" -> {
            if (args.isEmpty()) return "PUSHINT_4"
            val num = args[0].text.toIntOrNull() ?: return "PUSHINT_4"
            when (num) {
                in 0..15         -> "PUSHINT_4"
                in -128..127     -> "PUSHINT_8"
                in -32768..32767 -> "PUSHINT_16"
                else             -> "PUSHINT_LONG"
            }
        }

        "PUSH"    -> {
            if (args.size == 1 && args[0].text.startsWith("s")) return "PUSH"
            if (args.size == 2) return "PUSH2"
            if (args.size == 3) return "PUSH3"
            name
        }

        "XCHG0"   -> "XCHG_0I"
        "XCHG"    -> "XCHG_IJ"
        else      -> name
    }
}

fun getStackPresentation(rawStack: String?): String {
    if (rawStack.isNullOrBlank()) return ""
    val trimmed = rawStack.trim()
    val prefix = if (trimmed.startsWith("-")) "∅ " else ""
    val suffix = if (trimmed.endsWith("-")) " ∅" else ""
    val stack = prefix + rawStack.replace("-", "→") + suffix
    return "($stack)"
} 