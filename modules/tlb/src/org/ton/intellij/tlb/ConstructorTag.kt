package org.ton.intellij.tlb

import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.TlbConstructorTag
import org.ton.intellij.tlb.psi.printToString
import java.util.zip.CRC32

private const val SHORT_TAG_MASK = (1L shl 59) - 1
private const val TAG_MASK = (1L shl 63) - 1
private const val HEX = "0123456789abcdef"

class ConstructorTag(
    val value: Long
) {
    override fun toString(): String {
        if (value == 0L) {
            return "\$_"
        }
        return buildString {
            var tag = value
            if (SHORT_TAG_MASK and tag == 0L) {
                append('$')
                var c = 0
                while (tag and TAG_MASK != 0L) {
                    append(tag ushr 63)
                    tag = tag shl 1
                    c++
                }
                if (c == 0) {
                    append("_")
                }
            } else {
                append("#")
                while (tag and TAG_MASK != 0L) {
                    append(HEX[(tag ushr 60).toInt()])
                    tag = tag shl 4
                }
                if (tag == 0L) {
                    append("_")
                }
            }
        }
    }

    companion object {
        val EMPTY = ConstructorTag(0L)
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun TlbConstructor.computeTag(): ConstructorTag? {
    val normalized = printToString(skipTag = true, showBraces = false)
    if (normalized.contains("(invalid")) {
        return null
    }
    return CRC32().run {
        update(normalized.toByteArray())
//        println("crc32('$normalized') = ${value.toHexString()}")
        ConstructorTag((value.toLong() shl 32) or 0x80000000)
    }
}

fun TlbConstructorTag.toConstructorTag(): ConstructorTag {
    val str = binaryTag?.text ?: hexTag?.text ?: return ConstructorTag.EMPTY
    val n = str.length
    if (n <= 1) return ConstructorTag.EMPTY
    var i = 1
    var value = 0L
    var bits = 0

    when (str[0]) {
        '#' -> {
            while (i < n) {
                val c = str[i]
                if (c == '_') {
                    break
                }
                val hex = when (c) {
                    in '0'..'9' -> c.code - '0'.code
                    in 'A'..'F' -> c.code - 'A'.code + 10
                    in 'a'..'f' -> c.code - 'a'.code + 10
                    else -> return ConstructorTag.EMPTY
                }

                if (bits + 4 > 64) return ConstructorTag.EMPTY

                value = value or (hex.toLong() shl (60 - bits))
                bits += 4
                i++
            }
        }

        '$' -> {
            if (str[1] != '_') {
                while (i < n) {
                    val c = str[i]
                    if (c != '0' && c != '1') return ConstructorTag.EMPTY
                    if (bits == 64) return ConstructorTag.EMPTY

                    val bit = c.code - '0'.code
                    value = value or (bit.toLong() shl (63 - bits))
                    bits++
                    i++
                }
            }
        }
        else -> return ConstructorTag.EMPTY
    }

    if (i < n - 1) return ConstructorTag.EMPTY

    if (i == n - 1 && bits > 0) {
        while (bits > 0 && ((value shr (64 - bits)) and 1) == 0L) {
            bits--
        }
        if (bits > 0) {
            bits--
        }
    }

    if (bits == 64) return ConstructorTag.EMPTY

    return ConstructorTag(value or (1L shl (63 - bits)))
}