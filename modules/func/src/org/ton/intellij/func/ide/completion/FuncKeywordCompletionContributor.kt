package org.ton.intellij.func.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.patterns.PlatformPatterns.psiElement
import org.ton.intellij.func.psi.FuncBlockStatement
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncStatement

class FuncKeywordCompletionContributor : CompletionContributor() {
    init {
//        extend(
//            CompletionType.BASIC,
//            insideBlock(),
//        )
    }

    private fun insideBlock() {
        psiElement().withElementType(FuncElementTypes.IDENTIFIER)
            .withParent(
                psiElement(FuncStatement::class.java)
                    .inside(FuncBlockStatement::class.java)
            )
    }
}
