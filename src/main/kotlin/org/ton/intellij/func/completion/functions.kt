package org.ton.intellij.func.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.*
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.util.elementType
import com.intellij.psi.util.prevLeaf
import com.intellij.psi.util.prevLeafs
import com.intellij.util.ProcessingContext
import com.jetbrains.rd.util.measureTimeMillis
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.resolve.resolveAllFunctions
import org.ton.intellij.func.resolve.resolveReferenceExpressionProviders
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class FuncFunctionCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            psiElement().inside(psiElement().withElementType(FuncTokenTypes.BLOCK_STATEMENT)),
            FuncFunctionCompletionProvider()
        )
    }
}

class FuncFunctionCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val file = parameters.originalFile as? FuncFile ?: return
        val functions = file.resolveAllFunctions()

        functions.map { functionDef ->
            val parameterList = functionDef.parameterList.text
            val functionReturn = functionDef.functionReturnType.text
            LookupElementBuilder
                .createWithIcon(functionDef)
                .withTailText(parameterList)
                .withTypeText(functionReturn)
                .insertParenthesis(functionDef.parameterList.parameterDeclarationList.isEmpty())
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