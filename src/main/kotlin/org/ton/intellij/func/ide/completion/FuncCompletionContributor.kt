package org.ton.intellij.func.ide.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.or
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.*

class FuncCompletionContributor : CompletionContributor() {
    init {
        extend(FuncMacroCompletionProvider)
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
        extend(
            CompletionType.BASIC,
            psiElement().withParent(FuncFunctionParameter::class.java),
            FuncKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                keywords = funcPrimitiveTypes
            )
        )
        extend(
            CompletionType.BASIC,
            or(
                psiElement().inside(FuncTupleType::class.java),
                psiElement().inside(FuncTensorType::class.java),
                psiElement().inside(FuncParenType::class.java),
            ), FuncKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                keywords = funcPrimitiveTypes,
                insertSpace = false
            )
        )
        extend(
            CompletionType.BASIC,
            baseFunctionAttributePattern(
                psiElement(FuncElementTypes.RPAREN)
            ),
            FuncKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                "impure",
                "pure"
            )
        )
        extend(
            CompletionType.BASIC,
            baseFunctionAttributePattern(
                psiElement(FuncElementTypes.RPAREN),
                psiElement(FuncElementTypes.IMPURE_KEYWORD),
            ),
            FuncKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                "inline",
                "inline_ref",
            )
        )
        extend(
            CompletionType.BASIC,
            baseFunctionAttributePattern(
                psiElement(FuncElementTypes.RPAREN),
                psiElement(FuncElementTypes.IMPURE_KEYWORD),
                psiElement(FuncElementTypes.INLINE_KEYWORD),
                psiElement(FuncElementTypes.INLINE_REF_KEYWORD),
            ),
            FuncKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                "method_id",
            )
        )
        extend(
            CompletionType.BASIC,
            psiElement().afterLeaf(
                psiElement(FuncElementTypes.RBRACE).withAncestor(
                    2,
                    psiElement(FuncElementTypes.IF_STATEMENT)
                )
            ),
            FuncKeywordCompletionProvider(
                CONTEXT_KEYWORD_PRIORITY,
                "else",
                "elseif",
                "elseifnot"
            )
        )
        extend(FuncCommonCompletionProvider)
    }

    fun extend(provider: FuncCompletionProvider) {
        extend(CompletionType.BASIC, provider.elementPattern, provider)
    }

    private fun inBlock() =
        psiElement().withParent(
            psiElement(FuncReferenceExpression::class.java).withParent(
                psiElement(
                    FuncExpressionStatement::class.java
                ).inside(FuncBlockStatement::class.java)
            )
        )

    private fun baseFunctionAttributePattern(
        vararg afterLeafs: ElementPattern<out PsiElement>,
    ) = psiElement()
        .withAncestor(2, psiElement(FuncFunction::class.java))
        .afterLeaf(
            or(
                *afterLeafs
            )
        ).andNot(
            psiElement().beforeLeaf(psiElement(FuncElementTypes.IDENTIFIER))
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
