package com.github.andreypfau.intellijton.fift.completion

import com.github.andreypfau.intellijton.fift.psi.FiftFile
import com.github.andreypfau.intellijton.fift.psi.FiftTypes
import com.github.andreypfau.intellijton.fift.resolve.resolveAllWordDefs
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class FiftWordCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(FiftTypes.IDENTIFIER), FuncWordCompletionProvider())
    }
}

class FuncWordCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        if (parameters.position.text == CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED) return
        val file = parameters.originalFile as? FiftFile ?: return
        val textOffset = parameters.position.textOffset
        file.resolveAllWordDefs().filter {
            it.wordDefIdentifier.textOffset < textOffset
        }.map { wordDef ->
            LookupElementBuilder.createWithIcon(wordDef)
        }.forEach {
            result.addElement(it)
        }
    }
}