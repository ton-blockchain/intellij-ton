package org.ton.intellij.func.ide.completion

import com.intellij.patterns.PlatformPatterns.psiElement
import org.ton.intellij.func.psi.FuncBlockStatement
import org.ton.intellij.func.psi.FuncExpressionStatement
import org.ton.intellij.func.psi.FuncReferenceExpression

object FuncCompletionPatterns {
    fun inBlock() =
        psiElement().withParent(
            psiElement(FuncReferenceExpression::class.java).withParent(
                psiElement(
                    FuncExpressionStatement::class.java
                ).inside(FuncBlockStatement::class.java)
            )
        )
}
