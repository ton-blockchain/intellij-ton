package org.ton.intellij.tolk.type.infer

import org.ton.intellij.tolk.type.ty.TolkTy
import org.ton.intellij.tolk.type.ty.TolkTyUnknown

/**
 * When type-checking an expression, we propagate downward
 * whatever type hint we are able in the form of an [Expectation]
 */
sealed class Expectation {


    /** We know nothing about what type this expression should have */
    object NoExpectation : Expectation()

    /** This expression should have the type given (or some subtype) */
    data class ExpectHasTy(val ty: TolkTy) : Expectation()

    fun onlyHasTy(ctx: TolkInferenceContext): TolkTy? {
        return when (this) {
            is ExpectHasTy -> ctx.resolveTypeVarsIfPossible(ty)
            else -> return null
        }
    }
}

fun TolkTy?.maybeHasType(): Expectation =
    if (this == null || this is TolkTyUnknown) {
        Expectation.NoExpectation
    } else {
        Expectation.ExpectHasTy(this)
    }
