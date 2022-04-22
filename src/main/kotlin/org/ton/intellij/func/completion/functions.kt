package org.ton.intellij.func.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.resolve.resolveAllFunctions
import org.ton.intellij.func.resolve.resolveReferenceExpressionProviders

class FuncFunctionCompletionContributor : CompletionContributor(), DumbAware {
    init {
        extend(
            CompletionType.BASIC, StandardPatterns.or(
                PlatformPatterns.psiElement(FuncTokenTypes.IDENTIFIER).inside(FuncTensorExpression::class.java),
                PlatformPatterns.psiElement(FuncTokenTypes.IDENTIFIER).inside(FuncTupleExpression::class.java)
            ), FuncFunctionCompletionProvider(false)
        )
        extend(
            CompletionType.BASIC, StandardPatterns.or(
                PlatformPatterns.psiElement(FuncTokenTypes.IDENTIFIER).inside(FuncExpressionStatement::class.java)
            ), FuncFunctionCompletionProvider(true)
        )
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
    }
}

class FuncFunctionCompletionProvider(
    private val insertSemicolon: Boolean
) : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        if (parameters.position.text == CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED) return
        val file = parameters.originalFile as? FuncFile ?: return
        val functions = file.resolveAllFunctions().filter {
            it.functionName.firstChild.elementType === FuncTokenTypes.IDENTIFIER
        }
        functions.map { functionDef ->
            val parameterList = functionDef.parameterList.text
            val functionReturn = functionDef.functionReturnType.text
            LookupElementBuilder
                .createWithIcon(functionDef)
                .withTailText(parameterList)
                .withTypeText(functionReturn)
                .insertParenthesis(insertSemicolon, functionDef.parameterList.parameterDeclarationList.isEmpty())
        }.forEach {
            result.addElement(it)
        }
        file.resolveReferenceExpressionProviders(parameters.position.textOffset).map {
            LookupElementBuilder.createWithIcon(it)
        }.forEach {
            result.addElement(it)
        }
    }
}