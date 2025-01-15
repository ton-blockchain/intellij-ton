package org.ton.intellij.tact.psi

interface TactInferenceContextOwner : TactElement {
    val body: TactBlock?
}
