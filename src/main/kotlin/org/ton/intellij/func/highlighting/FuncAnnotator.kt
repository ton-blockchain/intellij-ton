package org.ton.intellij.func.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncTypeIdentifier
import org.ton.intellij.func.psi.FuncTypeParameter

class FuncAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!element.isValid) return
        when (element) {
            is FuncFunction -> {
                val identifier = element.nameIdentifier
                if (identifier != null) {
                    highlight(identifier, holder, FuncSyntaxHighlightingColors.FUNCTION_DECLARATION.textAttributesKey)
                }
            }

            is FuncTypeParameter -> {
                highlight(element.identifier, holder, FuncSyntaxHighlightingColors.TYPE_PARAMETER.textAttributesKey)
            }

            is FuncTypeIdentifier -> {
                highlight(element.identifier, holder, FuncSyntaxHighlightingColors.TYPE_PARAMETER.textAttributesKey)
            }
        }
    }

    private fun highlight(element: PsiElement, holder: AnnotationHolder, key: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.textRange)
            .textAttributes(key)
            .create()
    }
}
