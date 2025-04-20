package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.type.TolkStructType

object TolkDotExpressionCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = PlatformPatterns.psiElement()
            .afterLeaf(".")
            .withSuperParent(2, TolkDotExpression::class.java)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val dotExpression = parameters.position.parentOfType<TolkDotExpression>() ?: return
        val leftType = dotExpression.left.type ?: return
        val actualLeftType = leftType.unwrapTypeAlias().actualType()
        if (actualLeftType is TolkStructType) {
            actualLeftType.psi.structBody?.structFieldList?.forEach { field ->
                result.addElement(
                    LookupElementBuilder
                        .createWithIcon(field)
                        .apply {
                            field.type?.displayName?.let {
                                withTypeText(it)
                            }
                        }
                )
            }
        }
    }
}
