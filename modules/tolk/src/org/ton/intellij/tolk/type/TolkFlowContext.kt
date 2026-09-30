package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkSymbolElement
import org.ton.intellij.tolk.psi.TolkVar
import org.ton.intellij.tolk.psi.impl.*

class TolkFlowContext(
    val functions: MutableMap<String, MutableCollection<TolkFunction>> = HashMap(),
    val symbolTypes: MutableMap<TolkSymbolElement, TolkTy> = LinkedHashMap(),
    val symbols: MutableMap<String, TolkSymbolElement> = LinkedHashMap(),
    val sinkExpressions: MutableMap<TolkSinkExpression, TolkTy> = LinkedHashMap(),
    var unreachable: TolkUnreachableKind? = null,
) {
    constructor(other: TolkFlowContext) : this(
        HashMap(other.functions),
        LinkedHashMap(other.symbolTypes),
        LinkedHashMap(other.symbols),
        LinkedHashMap(other.sinkExpressions),
        other.unreachable,
    )

    fun clone() = TolkFlowContext(this)

    /** Compares reachability and inferred types to detect a loop's fixed point. */
    fun equivalentTo(other: TolkFlowContext): Boolean {
        if ((unreachable == null) != (other.unreachable == null) ||
            symbolTypes.size != other.symbolTypes.size ||
            sinkExpressions.size != other.sinkExpressions.size
        ) {
            return false
        }
        for ((symbol, type) in symbolTypes) {
            val otherType = other.symbolTypes[symbol] ?: return false
            if (!type.isEquivalentTo(otherType)) return false
        }
        for ((sink, type) in sinkExpressions) {
            val otherType = other.sinkExpressions[sink] ?: return false
            if (!type.isEquivalentTo(otherType)) return false
        }
        return true
    }

    fun getType(symbol: TolkSymbolElement): TolkTy? = symbolTypes[symbol]

    fun getType(sinkExpression: TolkSinkExpression): TolkTy? = sinkExpressions[sinkExpression]

    /** Returns the current smart cast, falling back to the type inferred for this expression. */
    fun smartcastOr(sinkExpression: TolkSinkExpression, originalType: TolkTy): TolkTy =
        sinkExpressions[sinkExpression] ?: originalType

    private fun declaredType(sink: TolkSinkExpression, ctx: TolkInferenceContext): TolkTy? {
        var current = if (sink.symbol is TolkVar) ctx.getType(sink.symbol) else sink.symbol.type
        var path = sink.indexPath
        while (path != 0L) {
            current = childType(current, ((path and 0xFF) - 1).toInt())
            path = path ushr 8
        }
        return current
    }

    private fun effectiveType(sink: TolkSinkExpression, ctx: TolkInferenceContext): TolkTy? {
        var currentSink = sink.copy(indexPath = 0)
        var current = sinkExpressions[currentSink] ?: symbolTypes[sink.symbol] ?: declaredType(currentSink, ctx)
        var remaining = sink.indexPath
        var shift = 0
        while (remaining != 0L) {
            val index = ((remaining and 0xFF) - 1).toInt()
            currentSink = currentSink.copy(indexPath = currentSink.indexPath or ((remaining and 0xFF) shl shift))
            current = sinkExpressions[currentSink] ?: childType(current, index) ?: return null
            remaining = remaining ushr 8
            shift += 8
        }
        return current
    }

    /** Restores source-level aliases after a merge without discarding narrower types from reachable paths. */
    fun reanchorTo(before: TolkFlowContext, ctx: TolkInferenceContext) {
        val iterator = sinkExpressions.iterator()
        while (iterator.hasNext()) {
            val (sink, type) = iterator.next()
            val typeBefore = before.effectiveType(sink, ctx)
            if (type.unwrapTypeAlias().isEquivalentTo(typeBefore?.unwrapTypeAlias())) {
                val factBefore = before.sinkExpressions[sink]
                if (factBefore != null) {
                    sinkExpressions[sink] = factBefore
                } else {
                    iterator.remove()
                }
                continue
            }
            val declared = declaredType(sink, ctx)
            if (type.unwrapTypeAlias().isEquivalentTo(declared?.unwrapTypeAlias())) {
                if (sink.indexPath == 0L) {
                    sinkExpressions[sink] = declared!!
                } else {
                    iterator.remove()
                }
            }
        }
    }

    private fun childType(parent: TolkTy?, index: Int): TolkTy? {
        var type = parent?.unwrapTypeAlias()
        if (type is TolkTyUnion) type = type.orNull?.unwrapTypeAlias() ?: type
        return when (type) {
            is TolkTyTensor -> type.elements.getOrNull(index)
            is TolkTyTypedTuple -> type.elements.getOrNull(index)
            is TolkTyStruct -> {
                val field = type.psi.structFields.getOrNull(index) ?: return null
                val sub = Substitution.instantiate(type.psi.declaredType, type)
                field.type?.substitute(sub)
            }
            else -> null
        }
    }

    fun getSymbol(name: String?): TolkSymbolElement? {
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
            indexMask = (indexMask shl 8) or 0xFF
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
            joinedUnreachable,
        )
    }
}

fun TolkFlowContext?.join(element: TolkFlowContext): TolkFlowContext = this?.join(element) ?: element
