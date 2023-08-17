package org.ton.intellij.func.psi.impl

import org.ton.intellij.func.psi.FuncVarExpression

val FuncVarExpression.right get() = expressionList.getOrNull(1)
