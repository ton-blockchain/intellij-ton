package org.ton.intellij.tolk.ide.completion

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns
import org.ton.intellij.tolk.psi.TolkBlockStatement
import org.ton.intellij.tolk.psi.TolkExpressionStatement
import org.ton.intellij.tolk.psi.TolkReferenceExpression

object TolkCompletionPatterns {
    fun inBlock() =
        psiElement().withParent(
            psiElement(TolkReferenceExpression::class.java).withParent(
                psiElement(
                    TolkExpressionStatement::class.java
                ).inside(TolkBlockStatement::class.java)
            )
        ).andNot(
            psiElement().afterLeaf(
                psiElement().withText(StandardPatterns.string().matches("\\d+"))
            )
        )
}
