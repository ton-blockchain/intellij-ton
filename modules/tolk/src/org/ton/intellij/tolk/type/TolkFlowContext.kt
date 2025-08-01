package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkSymbolElement

class TolkFlowContext(
    val functions: MutableMap<String, MutableCollection<TolkFunction>> = HashMap(),
    val symbolTypes: MutableMap<TolkSymbolElement, TolkTy> = LinkedHashMap(),
    val symbols: MutableMap<String, TolkSymbolElement> = LinkedHashMap(),
    val sinkExpressions: MutableMap<TolkSinkExpression, TolkTy> = LinkedHashMap(),
    var unreachable: TolkUnreachableKind? = null
) {
    constructor(other: TolkFlowContext) : this(
        HashMap(other.functions),
        LinkedHashMap(other.symbolTypes),
        LinkedHashMap(other.symbols),
        LinkedHashMap(other.sinkExpressions),
        other.unreachable
    )

    fun clone() = TolkFlowContext(this)

    fun getType(symbol: TolkSymbolElement): TolkTy? {
        return symbolTypes[symbol]
    }

    fun getType(sinkExpression: TolkSinkExpression): TolkTy? {
        return sinkExpressions[sinkExpression]
    }

    fun getSymbol(
        name: String?,
    ): TolkSymbolElement? {
        val fullName = name?.removeSurrounding("`") ?: return null
        return symbols[fullName]
    }

    fun setSymbol(element: TolkSymbolElement, type: TolkTy) {
        val name = element.name?.removeSurrounding("`") ?: return
        symbols[name] = element
        symbolTypes[element] = type
        invalidateAllSubfields(element, 0, 0)
    }

    fun setSymbol(element: TolkSinkExpression, type: TolkTy) {
        var indexPath = element.indexPath
//        if (indexPath < 0) {
//            return setSymbol(element.symbol, type)
//        }

        var indexMask = 0L
        while (indexPath > 0) {
            indexMask = (indexPath ushr 8) or 0xFF
            indexPath = indexPath ushr 8
        }
        invalidateAllSubfields(element.symbol, element.indexPath, indexMask)
        sinkExpressions[element] = type
    }

    private fun invalidateAllSubfields(element: TolkSymbolElement, parentPath: Long, parentMask: Long) {
        sinkExpressions.keys.removeAll {
            it.symbol == element && (it.indexPath and parentMask) == parentPath
        }
    }

    fun join(other: TolkFlowContext): TolkFlowContext {
        if (this.unreachable == null && other.unreachable != null) {
            return other.join(this)
        }

        val joinedSymbols: MutableMap<String, TolkSymbolElement> = HashMap(other.symbols)
        joinedSymbols.putAll(symbols)

        val joinedSinkExpressionsMutableMap: MutableMap<TolkSinkExpression, TolkTy>
        val joinedSymbolTypes: MutableMap<TolkSymbolElement, TolkTy>

        if (this.unreachable != null && other.unreachable == null) {
            joinedSymbolTypes = HashMap(symbolTypes)
            other.symbolTypes.forEach { otherSymbol ->
                joinedSymbolTypes[otherSymbol.key] = otherSymbol.value
            }
            joinedSinkExpressionsMutableMap = HashMap()
            other.sinkExpressions.forEach { otherSinkExpression ->
                joinedSinkExpressionsMutableMap[otherSinkExpression.key] = otherSinkExpression.value
            }
        } else {
            joinedSymbolTypes = HashMap(symbolTypes)
            other.symbolTypes.forEach { otherSymbol ->
                val a = joinedSymbolTypes[otherSymbol.key]
                val b = otherSymbol.value
                val result = a.join(b) ?: b
                joinedSymbolTypes[otherSymbol.key] = result
            }
            joinedSinkExpressionsMutableMap = HashMap()
            other.sinkExpressions.forEach { otherSExpr ->
                val a = sinkExpressions[otherSExpr.key]
                if (a != null) {
                    val b = otherSExpr.value
                    val result = a.join(b)
                    joinedSinkExpressionsMutableMap[otherSExpr.key] = result
                }
            }
        }

        val joinedUnreachable =
            if (unreachable != null && other.unreachable != null) TolkUnreachableKind.Unknown else null

        return TolkFlowContext(
            functions,
            joinedSymbolTypes,
            joinedSymbols,
            joinedSinkExpressionsMutableMap,
            joinedUnreachable
        )
    }
}

fun TolkFlowContext?.join(element: TolkFlowContext): TolkFlowContext {
    return this?.join(element) ?: element
}
