package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkIncludeDefinition
import org.ton.intellij.tolk.psi.TolkStringLiteral

object TolkImportStdlibCompletionProvider : TolkCompletionProvider(), DumbAware {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement()
            .withSuperParent(2, TolkStringLiteral::class.java)
            .and(
                psiElement().withSuperParent(3, TolkIncludeDefinition::class.java)
            )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        if (position.text != "IntellijIdeaRulezzz ") return // if not empty string literal

        result.addElement(
            LookupElementBuilder.create("@stdlib")
                .withTailText("/", true)
                .withIcon(AllIcons.Nodes.Folder)
                .withInsertHandler { context, _ ->
                    context.document.insertString(context.selectionEndOffset, "/")
                    context.editor.caretModel.moveToOffset(context.selectionEndOffset)
                    AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
                }
        )
    }
}
