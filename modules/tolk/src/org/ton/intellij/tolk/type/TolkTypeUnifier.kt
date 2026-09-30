package org.ton.intellij.tolk.type

/** Combines branch result types using the destination hint and Tolk's least common ancestor rules. */
internal class TolkTypeUnifier(hint: TolkTy?) {
    private val destinationHint = hint?.takeUnless { it == TolkTy.Unknown || it.hasGenerics() }

    var result: TolkTy? = null
        private set

    fun unifyWith(type: TolkTy) {
        var next = type
        val hint = destinationHint
        if (hint != null) {
            val union = hint.unwrapTypeAlias() as? TolkTyUnion
            if (union != null) {
                next = union.calculateExactVariantToFitRhs(next) ?: next
            } else if (hint.canRhsBeAssigned(next)) {
                next = hint
            }
        }
        val current = result
        result = if (current == null || current == next) next else calculateTypeLca(current, next).type
    }
}

private enum class TypeLcaStatus { NotAUnion, BecameUnion, FailedUnion }

private data class TypeLcaResult(val type: TolkTy, val status: TypeLcaStatus = TypeLcaStatus.NotAUnion)

private fun calculateTypeLca(left: TolkTy, right: TolkTy): TypeLcaResult {
    // Constant values are stored in types in the IDE and in separate flow facts in the compiler.
    val a = if (left is TolkIntTy || left is TolkTyBool) left.actualType() else left
    val b = if (right is TolkIntTy || right is TolkTyBool) right.actualType() else right
    if (a == TolkTy.Unknown || b == TolkTy.Unknown) return TypeLcaResult(TolkTy.Unknown)
    if (a == TolkTy.Never) return TypeLcaResult(b)
    if (b == TolkTy.Never) return TypeLcaResult(a)
    if (a == TolkTy.Null) return TypeLcaResult(b.nullable())
    if (b == TolkTy.Null) return TypeLcaResult(a.nullable())

    if (a is TolkTyTensor && b is TolkTyTensor && a.elements.size == b.elements.size) {
        val elements = a.elements.zip(b.elements).map { (x, y) -> calculateTypeLca(x, y) }
        if (elements.all { it.status == TypeLcaStatus.NotAUnion }) {
            return TypeLcaResult(TolkTyTensor.create(elements.map { it.type }))
        }
        return calculateUnionLca(a, b)
    }
    if (a is TolkTyAlias && b is TolkTyAlias && a.psi == b.psi && a.typeArguments == b.typeArguments) {
        return TypeLcaResult(a)
    }
    return calculateUnionLca(a, b)
}

private fun calculateUnionLca(a: TolkTy, b: TolkTy): TypeLcaResult {
    var invalidDuplicate = false
    val result = TolkTyUnion.createForLca(listOf(a, b)) { invalidDuplicate = true }
    val status = when {
        invalidDuplicate -> TypeLcaStatus.FailedUnion
        !result.isEquivalentTo(a) && !result.isEquivalentTo(b) -> TypeLcaStatus.BecameUnion
        else -> TypeLcaStatus.NotAUnion
    }
    return TypeLcaResult(result, status)
}
