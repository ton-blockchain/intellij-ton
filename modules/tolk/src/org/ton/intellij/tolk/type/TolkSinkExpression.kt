package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkSymbolElement

data class TolkSinkExpression(
    val symbol: TolkSymbolElement,
    val indexPath: Long = 0
) {
    override fun toString(): String = buildString {
        append(symbol.name)
        var curPath = indexPath
        while(curPath != 0L) {
            append('.')
            append((curPath and 0xFF) - 1)
            curPath = curPath ushr 8
        }
    }
}
