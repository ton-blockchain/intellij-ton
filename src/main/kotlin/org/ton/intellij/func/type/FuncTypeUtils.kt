package org.ton.intellij.func.type

import com.intellij.psi.PsiElement


open class FuncUnresolvedTypeException : RuntimeException {
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(message: String) : super(message)
}

class FuncInvalidStringTypeException(
    val psiElement: PsiElement,
) : FuncUnresolvedTypeException("Invalid string type '${psiElement.text}'")
