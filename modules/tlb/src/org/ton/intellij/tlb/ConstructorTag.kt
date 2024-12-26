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
            return ""
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
}

@OptIn(ExperimentalStdlibApi::class)
fun TlbConstructor.computeTag(): ConstructorTag? {
    val normalized = printToString(skipTag = true, showBraces = false)
    if (normalized.contains("(invalid")) {
        return null
    }
    return CRC32().run {
        update(normalized.toByteArray())
        println("crc32('$normalized') = ${value.toHexString()}")
        ConstructorTag((value.toLong() shl 32) or 0x80000000)
    }
}

fun TlbConstructorTag.toConstructorTag(): ConstructorTag {
    val str = binaryTag?.text ?: hexTag?.text
    var n = str?.length ?: 0
    if (str == null || n <= 1) {
        return ConstructorTag(0)
    }
    var bits = 0L
    var value = 0L
    if (str[0] == '#') {
        for (i in 1 until n) {
            var c = str[i]
            if (c =='_') {
                break
            }
            c = when (c) {
                in '0'..'9' -> c - '0'
                in 'a'..'f' -> c - 'a' + 10
                in 'A'..'F' -> c - 'A' + 10
                else -> return ConstructorTag(0)
            }.toChar()
            if (bits > 60) {
                return ConstructorTag(0)
            }
            value = (value shl 4) or c.toLong()
        }
    }

    return ConstructorTag(value)
}