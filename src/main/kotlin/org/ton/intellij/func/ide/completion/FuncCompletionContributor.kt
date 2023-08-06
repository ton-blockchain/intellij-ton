package org.ton.intellij.func.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement
import org.ton.intellij.func.psi.FuncBlockStatement
import org.ton.intellij.func.psi.FuncExpressionStatement
import org.ton.intellij.func.psi.FuncReferenceExpression

class FuncCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, referenceExpression(), FuncReferenceCompletionProvider())
        extend(
            CompletionType.BASIC,
            inBlock(),
            FuncKeywordCompletionProvider(
                KEYWORD_PRIORITY,
                "var",
                "if",
                "ifnot",
                "return",
                "repeat",
                "do",
                "while",
                "try"
            )
        )
    }

    fun extend(provider: FuncCompletionProvider) {
        extend(CompletionType.BASIC, provider.elementPattern, provider)
    }

    private fun referenceExpression() =
        psiElement().withParent(FuncReferenceExpression::class.java)

    private fun inBlock() =
        psiElement().withParent(
            psiElement(FuncReferenceExpression::class.java).withParent(
                psiElement(
                    FuncExpressionStatement::class.java
                ).inside(FuncBlockStatement::class.java)
            )
        )

    companion object {
        const val KEYWORD_PRIORITY = 20.0
        const val CONTEXT_KEYWORD_PRIORITY = 25.0
        const val NOT_IMPORTED_FUNCTION_PRIORITY = 3.0
        const val FUNCTION_PRIORITY = NOT_IMPORTED_FUNCTION_PRIORITY + 10.0
        const val NOT_IMPORTED_VAR_PRIORITY = 5.0
        const val VAR_PRIORITY = NOT_IMPORTED_VAR_PRIORITY + 10.0
    }
}
