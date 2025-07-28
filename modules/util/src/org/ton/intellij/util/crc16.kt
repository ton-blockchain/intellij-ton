package org.ton.intellij.util

private const val POLYNOMIAL = 0x1021
private val table: IntArray = makeCrc16Table(POLYNOMIAL)

private fun makeCrc16Table(polynomial: Int): IntArray {
    val table = IntArray(256)
    for (n in 0 until 256) {
        var crc = n shl 8
        repeat(8) {
            crc = if (crc and 0x8000 != 0) {
                (crc shl 1) xor polynomial
            } else {
                crc shl 1
            }
            crc = crc and 0xFFFF
        }
        table[n] = crc
    }
    return table
}

fun crc16(data: ByteArray): Int {
    var crc = 0
    for (byte in data) {
        val idx = ((crc shr 8) xor byte.toInt()) and 0xFF
        crc = ((crc shl 8) xor table[idx]) and 0xFFFF
    }
    return crc
}

fun crc16(text: String): Int =
    crc16(text.toByteArray(Charsets.UTF_8))
