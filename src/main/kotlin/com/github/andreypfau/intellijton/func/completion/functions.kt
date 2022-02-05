package com.github.andreypfau.intellijton.func.completion

import com.github.andreypfau.intellijton.func.resolve.FuncResolver
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.util.ProcessingContext

class FuncFunctionCompletionContributor : CompletionContributor(), DumbAware {
    init {
        extend(CompletionType.BASIC, block(), FuncFunctionCompletionProvider())
    }
}

class FuncFunctionCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        FuncResolver.resolveFunctions(parameters.originalFile).map {
            LookupElementBuilder.createWithIcon(it)
        }.forEach {
            result.addElement(it)
        }
    }
}