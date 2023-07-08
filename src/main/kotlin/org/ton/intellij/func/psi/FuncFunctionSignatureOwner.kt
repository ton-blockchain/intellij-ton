package org.ton.intellij.func.psi

interface FuncFunctionSignatureOwner {
    val isImpure: Boolean

    val isMutable: Boolean

    val hasMethodId: Boolean
}
