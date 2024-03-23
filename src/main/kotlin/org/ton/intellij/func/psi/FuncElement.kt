package org.ton.intellij.func.psi

import com.intellij.psi.PsiElement
import org.ton.intellij.func.type.infer.FuncInferenceResult
import org.ton.intellij.util.contexts
import org.ton.intellij.util.withNext

interface FuncElement : PsiElement

val FuncElement.inferenceContextOwner: FuncInferenceContextOwner?
    get() = contexts
        .withNext()
        .find { (it, next) ->
            next != null && it is FuncInferenceContextOwner
        }?.first as? FuncInferenceContextOwner

val FuncElement.inference: FuncInferenceResult?
    get() = inferenceContextOwner?.selfInferenceResult
