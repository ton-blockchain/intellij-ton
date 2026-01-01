package org.ton.intellij.acton.runconfig

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.util.TextFieldCompletionProvider
import org.ton.intellij.acton.cli.ActonToml

class ActonContractCompletionProvider(private val project: Project) : TextFieldCompletionProvider() {
    override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
        val actonToml = ActonToml.find(project) ?: return
        val contracts = actonToml.getContractIds()

        for (contract in contracts) {
            result.addElement(LookupElementBuilder.create(contract).withIcon(AllIcons.Nodes.Class))
        }
    }
}
