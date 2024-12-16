package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.patterns.PlatformPatterns.psiElement
import org.ton.intellij.tolk.psi.TolkBlockStatement
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkStatement

class TolkKeywordCompletionContributor : CompletionContributor() {
    init {
//        extend(
//            CompletionType.BASIC,
//            insideBlock(),
//        )
    }

    private fun insideBlock() {
        psiElement().withElementType(TolkElementTypes.IDENTIFIER)
            .withParent(
                psiElement(TolkStatement::class.java)
                    .inside(TolkBlockStatement::class.java)
            )
    }
}
