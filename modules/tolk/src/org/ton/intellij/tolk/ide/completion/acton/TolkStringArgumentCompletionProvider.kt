package org.ton.intellij.tolk.ide.completion.acton

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.ide.completion.TolkCompletionProvider
import org.ton.intellij.tolk.psi.*

abstract class TolkStringArgumentCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement(TolkElementTypes.RAW_STRING_ELEMENT)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val stringLiteral = PsiTreeUtil.getParentOfType(parameters.position, TolkStringLiteral::class.java) ?: return
        val literalExpression = stringLiteral.parent as? TolkLiteralExpression ?: return
        val argument = literalExpression.parent as? TolkArgument ?: return
        val argumentList = argument.parent as? TolkArgumentList ?: return
        val callExpression = argumentList.parent as? TolkCallExpression ?: return

        val functionName = getFunctionName(callExpression) ?: return
        val qualifierName = getQualifierName(callExpression)
        val argumentIndex = argumentList.argumentList.indexOf(argument)

        if (shouldAddCompletions(functionName, qualifierName, argumentIndex)) {
            addStringCompletions(parameters, context, result, functionName, qualifierName, argumentIndex)
        }
    }

    private fun getFunctionName(callExpression: TolkCallExpression): String? {
        val expr = callExpression.expression
        return when (expr) {
            is TolkReferenceExpression -> expr.referenceName
            is TolkDotExpression -> expr.fieldLookup?.identifier?.text
            else -> null
        }
    }

    private fun getQualifierName(callExpression: TolkCallExpression): String? {
        val expr = callExpression.expression
        if (expr is TolkDotExpression) {
            val qualifier = expr.expression
            if (qualifier is TolkReferenceExpression) {
                return qualifier.referenceName
            }
        }
        return null
    }

    abstract fun shouldAddCompletions(functionName: String, qualifierName: String?, argumentIndex: Int): Boolean

    abstract fun addStringCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
        functionName: String,
        qualifierName: String?,
        argumentIndex: Int
    )
}
