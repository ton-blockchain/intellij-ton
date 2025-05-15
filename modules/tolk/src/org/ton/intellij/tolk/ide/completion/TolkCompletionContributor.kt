package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.or
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import org.ton.intellij.tolk.psi.*

class TolkCompletionContributor : CompletionContributor() {
    init {
        extend(TolkMacroCompletionProvider)
        extend(TolkTypeCompletionProvider)
        extend(
            CompletionType.BASIC,
            inBlock(),
            TolkKeywordCompletionProvider(
                KEYWORD_PRIORITY,
                "var",
                "val",
                "if",
                "return",
                "repeat",
                "do",
                "while",
                "try"
            )
        )
        extend(TolkFunCompletionProvider)
        extend(TolkParameterCompletionProvider)
        extend(
            CompletionType.BASIC,
            psiElement().withParent(
                psiElement().withElementType(TokenType.ERROR_ELEMENT).withParent(TolkFile::class.java)
            ),
            TolkKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                "fun",
                "const",
                "global",
                "get",
                "tolk",
                "type",
                "struct",
            )
        )
        extend(
            CompletionType.BASIC,
            psiElement().afterLeaf(
                psiElement(TolkElementTypes.RBRACE).withAncestor(
                    2,
                    psiElement(TolkElementTypes.IF_STATEMENT)
                )
            ),
            TolkKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                "else",
            )
        )
        extend(
            CompletionType.BASIC,
            psiElement().afterLeaf(
                psiElement(TolkElementTypes.RBRACE).withAncestor(
                    2,
                    psiElement(TolkElementTypes.DO_STATEMENT)
                )
            ),
            TolkKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                "else",
            )
        )
        extend(TolkCommonCompletionProvider)
        extend(TolkDotExpressionCompletionProvider)
        extend(TolkExpressionFieldProvider)
    }

    fun extend(provider: TolkCompletionProvider) {
        extend(CompletionType.BASIC, provider.elementPattern, provider)
    }

    private fun inBlock() =
        psiElement().withParent(
            psiElement(TolkReferenceExpression::class.java).withParent(
                psiElement(
                    TolkExpressionStatement::class.java
                ).inside(TolkBlockStatement::class.java)
            )
        )

    private fun baseFunctionAttributePattern(
        vararg afterLeafs: ElementPattern<out PsiElement>,
    ) = psiElement()
        .withAncestor(2, psiElement(TolkFunction::class.java))
        .afterLeaf(
            or(
                *afterLeafs
            )
        ).andNot(
            psiElement().beforeLeaf(psiElement(TolkElementTypes.IDENTIFIER))
        )

    companion object {
        const val KEYWORD_PRIORITY = 20.0
        const val CONTEXT_KEYWORD_PRIORITY = 25.0
        const val NOT_IMPORTED_FUNCTION_PRIORITY = 3.0
        const val FUNCTION_PRIORITY = NOT_IMPORTED_FUNCTION_PRIORITY + 10.0
        const val NOT_IMPORTED_VAR_PRIORITY = 5.0
        const val VAR_PRIORITY = NOT_IMPORTED_VAR_PRIORITY + 10.0

        private val funcPrimitiveTypes
            get() = listOf(
                "cell",
                "builder",
                "slice",
                "int",
                "tuple",
                "continuation"
            )
    }
}
