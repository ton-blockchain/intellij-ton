package org.ton.intellij.tact.psi

interface TactFunctionLike : TactInferenceContextOwner {
    val functionParameters: TactFunctionParameters?
}
