package org.ton.intellij.acton.runconfig

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.TextFieldCompletionProvider

class ActonCommandCompletionProvider : TextFieldCompletionProvider() {
    override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
        // TODO: maybe we can expose API to get available commands?
        val commands = listOf(
            "init", "new", "wallet", "test", "wrapper", "script",
            "build", "run", "compile", "disasm", "verify", "retrace",
            "library", "up", "completions"
        )

        for (command in commands) {
            result.addElement(LookupElementBuilder.create(command))
        }
    }
}
