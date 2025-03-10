package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkSymbolElement

class TolkFlowContext(
    val globalSymbols: MutableMap<String, TolkSymbolElement> = HashMap(),
    val symbolTypes: MutableMap<TolkSymbolElement, TolkType> = LinkedHashMap(),
    val symbols: MutableMap<String, TolkSymbolElement> = LinkedHashMap(),
    val sinkExpressions: MutableMap<TolkSinkExpression, TolkType> = LinkedHashMap(),
    var unreachable: TolkUnreachableKind? = null
) {
    constructor(other: TolkFlowContext) : this(
        other.globalSymbols,
        LinkedHashMap(other.symbolTypes),
        LinkedHashMap(other.symbols),
        LinkedHashMap(other.sinkExpressions),
        other.unreachable
    )

    fun getType(symbol: TolkSymbolElement): TolkType? {
        return symbolTypes[symbol]
    }

    fun getType(sinkExpression: TolkSinkExpression): TolkType? {
        return sinkExpressions[sinkExpression]
    }

    fun getSymbol(
        name: String?,
    ): TolkSymbolElement? {
        val fullName = name?.removeSurrounding("`") ?: return null
        return symbols[fullName] ?: globalSymbols[fullName]
    }

    fun setSymbol(element: TolkSymbolElement, type: TolkType) {
        val name = element.name?.removeSurrounding("`") ?: return
        symbols[name] = element
        symbolTypes[element] = type
        invalidateAllSubfields(element, 0, 0)
    }

    fun setSymbol(element: TolkSinkExpression, type: TolkType) {
        var indexPath = element.indexPath
        if (indexPath <= 0) {
            return setSymbol(element.symbol, type)
        }

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
        if (other.unreachable == TolkUnreachableKind.CantHappen) {
            return TolkFlowContext(globalSymbols, symbolTypes, symbols, sinkExpressions, unreachable)
        }
        if (unreachable == TolkUnreachableKind.CantHappen) {
            return TolkFlowContext(
                other.globalSymbols,
                other.symbolTypes,
                other.symbols,
                other.sinkExpressions,
                other.unreachable
            )
        }
        val joinedSymbols = LinkedHashMap(symbolTypes)
        val joinedSinkExpressions = LinkedHashMap(sinkExpressions)
        val joinedNames = LinkedHashMap(symbols)
        other.symbolTypes.forEach { (element, type) ->
            joinedSymbols[element] = joinedSymbols[element]?.join(type) ?: type
        }
        other.symbols.forEach { (name, element) ->
            joinedNames[name] = element
        }
        other.sinkExpressions.forEach { (element, type) ->
            joinedSinkExpressions[element] = joinedSinkExpressions[element]?.join(type) ?: type
        }
        val joinedUnreachable =
            if (unreachable != null && other.unreachable != null) TolkUnreachableKind.Unknown else null
        return TolkFlowContext(globalSymbols, joinedSymbols, joinedNames, joinedSinkExpressions, joinedUnreachable)
    }
}
