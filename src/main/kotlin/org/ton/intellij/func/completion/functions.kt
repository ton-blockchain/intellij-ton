package org.ton.intellij.func.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.FuncTokenTypes
import org.ton.intellij.func.resolve.completeFiles
import org.ton.intellij.func.resolve.resolveAllFunctions
import org.ton.intellij.func.resolve.resolveReferenceExpressionProviders

class FuncFunctionCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            psiElement().inside(psiElement().withElementType(FuncTokenTypes.BLOCK_STATEMENT)),
            FuncFunctionCompletionProvider()
        )
        extend(
            CompletionType.BASIC,
            psiElement().inside(
                psiElement().withElementType(FuncTokenTypes.FUNCTION)
            ).andNot(
                psiElement().inside(
                    psiElement().withElementType(FuncTokenTypes.BLOCK_STATEMENT)
                )
            ),
            FuncFunctionSpecifierProvider()
        )
//        extend(
//            CompletionType.BASIC,
//            psiElement().inside(
//                psiElement().withElementType(FuncTokenTypes.STRING_LITERAL)
//            ),
//            FuncIncludePathCompletionProvider()
//        )
    }
}

class FuncIncludePathCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.originalFile as FuncFile
        val files = position.completeFiles()
        result.addAllElements(files)
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

        result.addElement(LookupElementBuilder.create("return").insertSpace())
        result.addElement(LookupElementBuilder.create("var").insertSpace())
        result.addElement(LookupElementBuilder.create("int").insertSpace())
        result.addElement(LookupElementBuilder.create("cell").insertSpace())
        result.addElement(LookupElementBuilder.create("slice").insertSpace())
        result.addElement(LookupElementBuilder.create("builder").insertSpace())
        result.addElement(LookupElementBuilder.create("cont").insertSpace())
        result.addElement(LookupElementBuilder.create("tuple").insertSpace())
        result.addElement(LookupElementBuilder.create("true"))
        result.addElement(LookupElementBuilder.create("false"))
        result.addElement(LookupElementBuilder.create("if").insertParenthesis(false))
        result.addElement(LookupElementBuilder.create("ifnot").insertParenthesis(false))
        result.addElement(LookupElementBuilder.create("while").insertParenthesis(false))
        result.addElement(LookupElementBuilder.create("repeat").insertParenthesis(false))
        result.addElement(LookupElementBuilder.create("do").insertBraces())

        functions.asSequence().mapNotNull { functionDef ->
            val parameterList = functionDef.parameterList ?: return@mapNotNull null
            val functionReturn = functionDef.functionReturnType.text
            LookupElementBuilder
                .createWithIcon(functionDef)
                .withTailText(parameterList.text)
                .withTypeText(functionReturn)
                .insertParenthesis(parameterList.parameterDeclarationList.isEmpty(), false)
        }.distinctBy {
            it.lookupString
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

class FuncFunctionSpecifierProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        result.addElement(LookupElementBuilder.create("impure").insertSpace())
        result.addElement(LookupElementBuilder.create("inline").insertSpace())
        result.addElement(LookupElementBuilder.create("inline_ref").insertSpace())
        result.addElement(LookupElementBuilder.create("asm").insertSpace())
        result.addElement(LookupElementBuilder.create("method_id"))
    }
}