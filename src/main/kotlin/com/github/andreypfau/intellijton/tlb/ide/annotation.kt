package com.github.andreypfau.intellijton.tlb.ide

import com.github.andreypfau.intellijton.tlb.psi.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class TlbAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is TlbConstructorName -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.identifier)
                .textAttributes(TlbColor.CONSTRUCTOR_NAME.textAttributesKey)
                .create()
            is TlbParam -> {
                val identifier = element.identifier
                if (identifier != null) {
                    holder
                        .newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(identifier)
                        .textAttributes(TlbColor.PARAMETER_NAME.textAttributesKey)
                        .create()
                }
            }
            is TlbCombinatorName -> holder
                .newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.identifier)
                .textAttributes(TlbColor.COMBINATOR_NAME.textAttributesKey)
                .create()
            is TlbTypeExpression -> {
                val identifier = element.identifier
                if (identifier != null && element.parent !is TlbCombinator) {
                    val text = identifier.text
                    val first = text.first()
                    if (text.length > 1 && first == first.uppercaseChar()) {
                        holder
                            .newSilentAnnotation(HighlightSeverity.INFORMATION)
                            .range(identifier)
                            .textAttributes(TlbColor.TYPE.textAttributesKey)
                            .create()
                    }
                }
            }
        }
    }
}