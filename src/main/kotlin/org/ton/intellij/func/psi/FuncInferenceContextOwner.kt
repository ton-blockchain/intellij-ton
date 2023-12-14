package org.ton.intellij.func.psi

import org.ton.intellij.util.contexts
import org.ton.intellij.util.withNext

interface FuncInferenceContextOwner : FuncElement

val FuncElement.inferenceContextOwner: FuncInferenceContextOwner?
    get() = contexts
        .withNext()
        .find { (it, next) ->
            next != null && it is FuncInferenceContextOwner
        }?.first as? FuncInferenceContextOwner
