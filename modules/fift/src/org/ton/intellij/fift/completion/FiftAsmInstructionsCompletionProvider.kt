package org.ton.intellij.fift.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import org.ton.intellij.util.asm.AsmDataProvider
import org.ton.intellij.util.asm.getStackPresentation

object FiftAsmInstructionsCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val data = AsmDataProvider.getAsmData()

        data.instructions.forEach { instr ->
            val name = adjustName(instr.mnemonic)
            val stack = getStackPresentation(instr.doc.stack)

            val element = LookupElementBuilder.create(name)
                .bold()
                .withTailText(" $stack", true)
                .withTypeText(instr.doc.gas)

            result.addElement(element)
        }

        data.aliases.forEach { alias ->
            val name = adjustName(alias.mnemonic)
            val stack = getStackPresentation(alias.docStack)

            val element = LookupElementBuilder.create(name)
                .bold()
                .withTailText(" $stack, alias of ${alias.aliasOf}", true)

            result.addElement(element)
        }
    }

    private fun adjustName(name: String): String {
        if (name.startsWith("PUSHINT_")) return "PUSHINT"
        if (name == "XCHG_0I") return "XCHG0"
        if (name == "XCHG_IJ") return "XCHG"
        if (name == "XCHG_0I_LONG") return "XCHG"
        if (name == "XCHG_1I") return "XCHG"
        return name
    }
}
