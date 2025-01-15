package org.ton.intellij.util.exception

import com.intellij.psi.PsiElement

class ConstantEvaluationOverflowException(val overflowingExpression: PsiElement) : RuntimeException()
