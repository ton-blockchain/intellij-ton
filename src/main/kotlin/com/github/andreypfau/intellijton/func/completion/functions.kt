package com.github.andreypfau.intellijton.func.completion

import com.github.andreypfau.intellijton.func.psi.*
import com.github.andreypfau.intellijton.func.resolve.FuncResolver
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.execution.impl.DUMMY_ELEMENT_NAME
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.util.ProcessingContext

class FuncFunctionCompletionContributor : CompletionContributor(), DumbAware {
    init {
        extend(CompletionType.BASIC, StandardPatterns.or(
            PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncParenthesizedExpression::class.java)
        ), FuncFunctionCompletionProvider(false))
        extend(CompletionType.BASIC, StandardPatterns.or(
            PlatformPatterns.psiElement(FuncTypes.IDENTIFIER).inside(FuncExpressionStatement::class.java)
        ), FuncFunctionCompletionProvider(true))
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

        val stdlibFunctions = FuncResolver.resolveStdlibFile(parameters.originalFile)?.let {
            FuncResolver.resolveFunctions(it)
        } ?: emptySequence()
        val fileFunctions = FuncResolver.resolveFunctions(parameters.originalFile)

        (stdlibFunctions + fileFunctions).map { functionDef ->
            val parameterList = functionDef.parameterList.text
            val functionReturn = functionDef.functionReturn.text
            LookupElementBuilder
                .createWithIcon(functionDef)
                .withTailText(parameterList)
                .withTypeText(functionReturn)
                .insertParenthesis(insertSemicolon, functionDef.parameterList.parameterDeclarationList.isEmpty())
        }.forEach {
            result.addElement(it)
        }
    }
}