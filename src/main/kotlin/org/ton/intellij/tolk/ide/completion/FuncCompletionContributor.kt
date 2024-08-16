package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.or
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.*

class TolkCompletionContributor : CompletionContributor() {
    init {
        extend(TolkMacroCompletionProvider)
        extend(
            CompletionType.BASIC,
            inBlock(),
            TolkKeywordCompletionProvider(
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
        extend(
            CompletionType.BASIC,
            psiElement().withParent(TolkFunctionParameter::class.java),
            TolkKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                keywords = funcPrimitiveTypes
            )
        )
        extend(
            CompletionType.BASIC,
            or(
                psiElement().inside(TolkTupleType::class.java),
                psiElement().inside(TolkTensorType::class.java),
                psiElement().inside(TolkParenType::class.java),
            ), TolkKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                keywords = funcPrimitiveTypes,
                insertSpace = false
            )
        )
        extend(
            CompletionType.BASIC,
            baseFunctionAttributePattern(
                psiElement(TolkElementTypes.RPAREN)
            ),
            TolkKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                "pure",
            )
        )
        extend(
            CompletionType.BASIC,
            baseFunctionAttributePattern(
                psiElement(TolkElementTypes.RPAREN),
                psiElement(TolkElementTypes.IMPURE_KEYWORD),
            ),
            TolkKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                "inline",
                "inline_ref",
            )
        )
        extend(
            CompletionType.BASIC,
            baseFunctionAttributePattern(
                psiElement(TolkElementTypes.RPAREN),
                psiElement(TolkElementTypes.IMPURE_KEYWORD),
                psiElement(TolkElementTypes.INLINE_KEYWORD),
                psiElement(TolkElementTypes.INLINE_REF_KEYWORD),
            ),
            TolkKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                "method_id",
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
                "elseif",
                "elseifnot"
            )
        )
        extend(TolkCommonCompletionProvider)
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
                "cont"
            )
    }
}
