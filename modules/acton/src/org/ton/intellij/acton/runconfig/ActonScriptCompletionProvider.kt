package org.ton.intellij.acton.runconfig

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.util.TextFieldCompletionProvider
import org.ton.intellij.acton.cli.ActonToml

class ActonScriptCompletionProvider(private val project: Project) : TextFieldCompletionProvider() {
    override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
        val actonToml = ActonToml.find(project) ?: return
        val scripts = actonToml.getScripts()

        for ((name, definition) in scripts) {
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(AllIcons.Actions.Execute)
                    .withTailText("  $definition", true)
            )
        }
    }
}
