package org.ton.intellij.tact.psi

interface TactFunctionLike : TactElement {
    val functionParameters: TactFunctionParameters?
}
