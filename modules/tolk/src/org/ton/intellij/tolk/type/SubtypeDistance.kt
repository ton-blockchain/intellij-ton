package org.ton.intellij.tolk.type

/**
 * Counts alias-unwrapping steps to a method receiver, or returns null for an incompatible direction.
 * Container children contribute their distances; union variants match by runtime type.
 */
fun TolkTy.subtypeDistanceTo(receiver: TolkTy): Int? {
    if (!isEquivalentTo(receiver)) return null

    if (this is TolkTyAlias) {
        if (receiver is TolkTyAlias) {
            if (psi == receiver.psi && typeArguments == receiver.typeArguments) return 0
            if (psi == receiver.psi && typeArguments.isNotEmpty() && receiver.typeArguments.isNotEmpty()) {
                var sum = 0
                for ((provided, target) in typeArguments.zip(receiver.typeArguments)) {
                    sum += provided.subtypeDistanceTo(target) ?: return null
                }
                return sum
            }
        }
        return (underlyingType.subtypeDistanceTo(receiver) ?: return null) + 1
    }
    if (receiver is TolkTyAlias) return null

    if (this is TolkTyUnion && receiver is TolkTyUnion) {
        var sum = 0
        for (target in receiver.variants) {
            val provided = variants.find { it.isEquivalentTo(target) } ?: return null
            sum += provided.subtypeDistanceTo(target) ?: return null
        }
        return sum
    }

    var sum = 0
    for ((provided, target) in subtypeChildren().zip(receiver.subtypeChildren())) {
        sum += provided.subtypeDistanceTo(target) ?: return null
    }
    return sum
}

private fun TolkTy.subtypeChildren(): List<TolkTy> = when (this) {
    is TolkTyArray -> listOf(elementType)
    is TolkTyTypedTuple -> elements
    is TolkTyTensor -> elements
    is TolkTyFunction -> parametersType + returnType
    is TolkTyStruct -> typeArguments
    else -> emptyList()
}

/** Accepts directional alias conversions first and other implicit coercions last. */
internal fun TolkTy.receiverDistanceFrom(provided: TolkTy): Int? {
    if (this == provided) return 0
    val distance = provided.subtypeDistanceTo(this)
    if (distance != null) return distance
    if (!isEquivalentTo(provided) && canRhsBeAssigned(provided) && this !is TolkTyAlias) return 1_000_000
    return null
}
