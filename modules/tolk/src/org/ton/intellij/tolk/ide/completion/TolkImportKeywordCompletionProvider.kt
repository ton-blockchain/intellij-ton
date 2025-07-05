package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkFile

object TolkImportKeywordCompletionProvider : TolkCompletionProvider(), DumbAware {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement()
            .afterLeafSkipping(psiElement().withText(""), psiElement().whitespaceCommentEmptyOrError())
            .withSuperParent(2, psiElement(TolkFile::class.java))

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        result.addElement(
            LookupElementBuilder.create("import").bold().withInsertHandler { context, item ->
                context.document.insertString(context.selectionEndOffset, " \"\"")
                context.editor.caretModel.moveToOffset(context.selectionEndOffset - 1)
                AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
            }
        )
    }
}
