package org.ton.intellij.tolk.psi

import com.intellij.psi.PsiElement


interface TolkElement : PsiElement

//val TolkElement.inferenceContextOwner: TolkInferenceContextOwner?
//    get() = contexts
//        .withNext()
//        .find { (it, next) ->
//            next != null && it is TolkInferenceContextOwner
//        }?.first as? TolkInferenceContextOwner
//
//val TolkElement.inference: TolkInferenceResult?
//    get() = inferenceContextOwner?.selfInferenceResult
