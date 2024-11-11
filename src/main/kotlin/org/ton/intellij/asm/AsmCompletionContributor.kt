package org.ton.intellij.asm

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class AsmCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withElementType(AsmElementTypes.UNKNOWN_IDENTIFIER),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    resultSet: CompletionResultSet
                ) {
                    AsmInstructionsCsv.INSTRUCTIONS.forEach {
                        resultSet.addLookupElement(
                            it.name,
                            it.docDescription,
                            if (it.docGas != "null") "Gas: ${it.docGas}" else null
                        )
                    }
                }
            })
    }

    private fun CompletionResultSet.addLookupElement(name: String, description: String, type: String?) {
        addElement(
            LookupElementBuilder.create(name)
                .appendTailText(" $description", true)
                .withTypeText(type, false)
        )
    }
}
