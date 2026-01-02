package org.ton.intellij.tolk.acton

import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.isGetMethod
import org.ton.intellij.tolk.psi.impl.isTestFunction
import org.ton.intellij.tolk.stub.index.TolkFunctionIndex

class TolkStringArgumentReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            psiElement(TolkStringLiteral::class.java),
            TolkStringArgumentReferenceProvider()
        )
    }
}

class TolkStringArgumentReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val stringLiteral = element as? TolkStringLiteral ?: return PsiReference.EMPTY_ARRAY
        val literalExpression = stringLiteral.parent as? TolkLiteralExpression ?: return PsiReference.EMPTY_ARRAY
        val argument = literalExpression.parent as? TolkArgument ?: return PsiReference.EMPTY_ARRAY
        val argumentList = argument.parent as? TolkArgumentList ?: return PsiReference.EMPTY_ARRAY
        val callExpression = argumentList.parent as? TolkCallExpression ?: return PsiReference.EMPTY_ARRAY

        val functionName = getFunctionName(callExpression) ?: return PsiReference.EMPTY_ARRAY
        val qualifierName = getQualifierName(callExpression)
        val argumentIndex = argumentList.argumentList.indexOf(argument)

        val rawString = stringLiteral.rawString ?: return PsiReference.EMPTY_ARRAY
        val range = TextRange(1, rawString.textLength + 1)

        if (functionName == "wallet" && qualifierName == "net" && argumentIndex == 0) {
            return arrayOf(TolkWalletReference(stringLiteral, range))
        }

        if (functionName == "build" && qualifierName == null && argumentIndex == 0) {
            return arrayOf(TolkContractReference(stringLiteral, range))
        }

        if (functionName == "runGetMethod" && qualifierName == "net" && argumentIndex == 1) {
            return arrayOf(TolkGetMethodReference(stringLiteral, range))
        }

        return PsiReference.EMPTY_ARRAY
    }

    private fun getFunctionName(callExpression: TolkCallExpression): String? {
        val expr = callExpression.expression
        return when (expr) {
            is TolkReferenceExpression -> expr.referenceName
            is TolkDotExpression       -> expr.fieldLookup?.identifier?.text
            else                       -> null
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
}

class TolkWalletReference(element: TolkStringLiteral, range: TextRange) :
    PsiReferenceBase<TolkStringLiteral>(element, range), HighlightedReference {
    override fun resolve(): PsiElement? {
        val project = element.project
        val targetName = element.text.removeSurrounding("\"")
        val actonToml = ActonToml.find(project) ?: return null
        return actonToml.getWallets().find { it.name == targetName }?.element
    }

    override fun getVariants(): Array<Any> = emptyArray()
}

class TolkContractReference(element: TolkStringLiteral, range: TextRange) :
    PsiReferenceBase<TolkStringLiteral>(element, range), HighlightedReference {
    override fun resolve(): PsiElement? {
        val project = element.project
        val targetName = element.text.removeSurrounding("\"")
        val actonToml = ActonToml.find(project) ?: return null
        return actonToml.getContractElements().find { it.name == targetName }
    }

    override fun getVariants(): Array<Any> = emptyArray()
}

class TolkGetMethodReference(element: TolkStringLiteral, range: TextRange) :
    PsiReferenceBase<TolkStringLiteral>(element, range), HighlightedReference {
    override fun resolve(): PsiElement? {
        val project = element.project
        val targetName = element.text.removeSurrounding("\"")
        var result: PsiElement? = null
        TolkFunctionIndex.processElements(project, targetName) { function ->
            if (function.isGetMethod && !function.isTestFunction()) {
                val file = function.containingFile.originalFile as? TolkFile
                if (file?.isActonFile() == false) {
                    result = function
                    return@processElements false
                }
            }
            true
        }
        return result
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
