package org.ton.intellij.func.type.infer

import org.ton.intellij.func.type.ty.FuncTy
import org.ton.intellij.func.type.ty.FuncTyUnknown

/**
 * When type-checking an expression, we propagate downward
 * whatever type hint we are able in the form of an [Expectation]
 */
sealed class Expectation {


    /** We know nothing about what type this expression should have */
    object NoExpectation : Expectation()

    /** This expression should have the type given (or some subtype) */
    data class ExpectHasTy(val ty: FuncTy) : Expectation()

    fun onlyHasTy(ctx: FuncInferenceContext): FuncTy? {
        return when (this) {
            is ExpectHasTy -> ctx.resolveTypeVarsIfPossible(ty)
            else -> return null
        }
    }
}

fun FuncTy?.maybeHasType(): Expectation =
    if (this == null || this is FuncTyUnknown) {
        Expectation.NoExpectation
    } else {
        Expectation.ExpectHasTy(this)
    }
