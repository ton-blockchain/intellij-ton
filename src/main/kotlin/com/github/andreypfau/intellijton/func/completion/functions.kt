package com.github.andreypfau.intellijton.func.completion

import com.github.andreypfau.intellijton.func.resolve.FuncResolver
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.util.ProcessingContext

class FuncFunctionCompletionContributor : CompletionContributor(), DumbAware {
    init {
//        extend(CompletionType.BASIC, expression(), FuncFunctionCompletionProvider(false))
//        extend(CompletionType.BASIC, block(), FuncFunctionCompletionProvider(true))
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
//        FuncResolver.resolveFunctions(parameters.originalFile).map {
//            val paramList = it.parameterList?.parameterDefList?.joinToString() ?: ""
//            LookupElementBuilder
//                .createWithIcon(it)
//                .withTailText("($paramList)")
//                .withTypeText(it.returnDef.toString())
//                .insertParenthesis(insertSemicolon)
//        }.forEach {
//            result.addElement(it)
//        }
    }
}