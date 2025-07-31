package org.ton.intellij.tolk.psi

import com.intellij.psi.PsiElement

interface TolkElement : PsiElement

fun TolkExpression.unwrapParentheses(): TolkExpression? {
    var current: TolkExpression? = this
    while (current is TolkParenExpression) {
        current = current.expression
    }
    return current
}
