package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.util.psiElement

object TolkParameterCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> = psiElement<PsiElement>()
        .withParent(psiElement<PsiElement>().withElementType(TolkElementTypes.PARAMETER))

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val parameter = position.parent as? TolkParameter ?: return
        if (parameter.nameIdentifier == position && parameter.mutateKeyword == null) {
            result.addElement(
                LookupElementBuilder.create("mutate").withBoldness(true)
                    .withInsertHandler { context, item ->
                        val editor = context.editor
                        val currentOffset = editor.caretModel.offset
                        val documentText = editor.document.immutableCharSequence
                        if (documentText.length <= currentOffset || documentText[currentOffset] != ' ') {
                            EditorModificationUtil.insertStringAtCaret(editor, " ")
                        }
                    }
            )
        }
    }
}