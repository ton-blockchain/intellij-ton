package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.psi.TolkIncludeDefinition
import org.ton.intellij.tolk.psi.TolkStringLiteral

object TolkImportMappingsCompletionProvider : TolkCompletionProvider(), DumbAware {
    override val elementPattern: ElementPattern<out PsiElement> = psiElement().withSuperParent(2, TolkStringLiteral::class.java).and(
        psiElement().withSuperParent(3, TolkIncludeDefinition::class.java)
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        val text = position.text.removeSuffix("IntellijIdeaRulezzz ")
        // only at the beginning of the string literal (possibly after @)
        if (text != "" && text != "@") return

        val project = position.project
        val mappings = mutableListOf("@stdlib")
        val actonToml = ActonToml.find(project)
        if (actonToml != null) {
            mappings.addAll(actonToml.getMappings().keys.map { "@$it" })
        }

        val resultSet = if (text == "@") result.withPrefixMatcher("@") else result
        for (mapping in mappings) {
            val element = LookupElementBuilder.create(mapping).withTailText("/", true).withIcon(AllIcons.Nodes.Folder)
                .withInsertHandler { context, _ ->
                    val range = TextRange(context.selectionEndOffset, context.selectionEndOffset + 1)
                    if (!context.document.getText(range).startsWith("/")) {
                        context.document.insertString(context.selectionEndOffset, "/")
                    }
                    context.editor.caretModel.moveToOffset(context.selectionEndOffset)
                    AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
                }

            resultSet.addElement(element)
        }
    }
}
