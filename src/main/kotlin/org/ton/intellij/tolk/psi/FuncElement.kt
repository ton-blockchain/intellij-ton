package org.ton.intellij.tolk.psi

import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.type.infer.TolkInferenceResult
import org.ton.intellij.util.contexts
import org.ton.intellij.util.withNext

interface TolkElement : PsiElement

val TolkElement.inferenceContextOwner: TolkInferenceContextOwner?
    get() = contexts
        .withNext()
        .find { (it, next) ->
            next != null && it is TolkInferenceContextOwner
        }?.first as? TolkInferenceContextOwner

val TolkElement.inference: TolkInferenceResult?
    get() = inferenceContextOwner?.selfInferenceResult
